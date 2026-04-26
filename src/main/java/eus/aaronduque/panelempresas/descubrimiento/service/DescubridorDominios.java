package eus.aaronduque.panelempresas.descubrimiento.service;

import eus.aaronduque.panelempresas.descubrimiento.dto.BraveSearchResponse;
import eus.aaronduque.panelempresas.descubrimiento.dto.ResultadoDescubrimiento;
import eus.aaronduque.panelempresas.empresas.entity.Empresa;
import eus.aaronduque.panelempresas.empresas.repository.EmpresaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * Orquesta el descubrimiento del dominio oficial de una empresa
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DescubridorDominios {

    private final EmpresaRepository empresaRepository;
    private final ClienteBrave clienteBrave;
    private final FiltroDominios filtroDominios;
    private final PersistenciaDescubrimiento persistencia;

    /**
     * Plantilla de query que mandamos a Brave.
     */
    private static final String PLANTILLA_QUERY = "%s web oficial";

    /**
     * Descubre el dominio oficial de una empresa
     */
    public ResultadoDescubrimiento descubrir(Long empresaId) {
        long inicio = System.currentTimeMillis();
        log.info("=== Iniciando descubrimiento de dominio para empresa {} ===", empresaId);

        // 1. Cargar empresa (la única excepción que sí lanzamos)
        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Empresa no encontrada: " + empresaId));

        String query = String.format(PLANTILLA_QUERY, empresa.getNombre());

        // 2. Llamar a Brave
        Optional<BraveSearchResponse> respuestaOpt;
        try {
            respuestaOpt = clienteBrave.buscar(query);
        } catch (Exception e) {
            // Defensa extra: ClienteBrave ya captura sus propias excepciones,
            // pero si algo se nos cuela, no rompemos el flujo.
            log.error("Excepción inesperada llamando a Brave para empresa {}: {}",
                empresaId, e.getMessage(), e);
            return construirError(empresa, query,
                "Excepción inesperada en ClienteBrave: " + e.getMessage(),
                inicio);
        }

        if (respuestaOpt.isEmpty()) {
            // Fallo de red, timeout, 4xx/5xx de Brave, JSON malformado...
            log.warn("Brave no devolvió respuesta utilizable para empresa {}: '{}'",
                empresaId, query);
            return construirError(empresa, query,
                "Brave Search no devolvió respuesta (timeout, error HTTP o JSON inválido)",
                inicio);
        }

        BraveSearchResponse respuesta = respuestaOpt.get();
        int totalResultados = contarResultados(respuesta);

        // 3. Filtrar
        FiltroDominios.ResultadoFiltro filtrado;
        try {
            filtrado = filtroDominios.filtrar(respuesta);
        } catch (Exception e) {
            log.error("Error filtrando resultados para empresa {}: {}",
                empresaId, e.getMessage(), e);
            return construirError(empresa, query,
                "Error en filtro de dominios: " + e.getMessage(),
                inicio);
        }

        // 4a. Sin resultado tras filtrar → marcar empresa como sin_web
        if (filtrado.elegido().isEmpty()) {
            try {
                persistencia.marcarSinWeb(empresa, query, filtrado.dominiosDescartados());
            } catch (Exception e) {
                // Si falla la BD aquí, lo registramos pero devolvemos sin_resultado
                // igualmente (la información para el usuario es la misma).
                log.error("Error marcando empresa {} como sin_web: {}",
                    empresaId, e.getMessage(), e);
                return construirError(empresa, query,
                    "Error al marcar empresa como sin_web: " + e.getMessage(),
                    inicio);
            }

            long duracion = System.currentTimeMillis() - inicio;
            log.info("=== Empresa {} sin dominio válido ({}ms) ===", empresaId, duracion);
            return ResultadoDescubrimiento.builder()
                .empresaId(empresaId)
                .nombreEmpresa(empresa.getNombre())
                .estado(ResultadoDescubrimiento.Estado.sin_resultado)
                .queryUsada(query)
                .resultadosBrave(totalResultados)
                .resultadosTrasFiltro(0)
                .dominiosDescartados(filtrado.dominiosDescartados())
                .duracionMs(duracion)
                .mensaje(construirMensajeSinResultado(totalResultados,
                    filtrado.dominiosDescartados()))
                .build();
        }

        // 4b. Hay ganador → guardar dominio
        BraveSearchResponse.Result ganador = filtrado.elegido().get();
        String urlCompleta = ganador.getUrl();
        String dominio = extraerDominio(urlCompleta);

        if (dominio == null) {
            // Edge case: el filtro nos dio un resultado pero la URL no es parseable.
            // Es prácticamente imposible (el filtro ya extrae el dominio internamente),
            // pero lo manejamos por defensa.
            log.error("Resultado ganador con URL no parseable: {}", urlCompleta);
            return construirError(empresa, query,
                "URL del resultado ganador no parseable: " + urlCompleta,
                inicio);
        }

        try {
            persistencia.guardarDominio(empresa, dominio, urlCompleta);
        } catch (Exception e) {
            log.error("Error guardando dominio para empresa {}: {}",
                empresaId, e.getMessage(), e);
            return construirError(empresa, query,
                "Error guardando dominio en BD: " + e.getMessage(),
                inicio);
        }

        long duracion = System.currentTimeMillis() - inicio;
        log.info("=== Empresa {} dominio descubierto: {} ({}ms) ===",
            empresaId, dominio, duracion);

        return ResultadoDescubrimiento.builder()
            .empresaId(empresaId)
            .nombreEmpresa(empresa.getNombre())
            .estado(ResultadoDescubrimiento.Estado.encontrado)
            .queryUsada(query)
            .dominioEncontrado(dominio)
            .urlCompletaEncontrada(urlCompleta)
            .resultadosBrave(totalResultados)
            .resultadosTrasFiltro(totalResultados - filtrado.dominiosDescartados().size())
            .dominiosDescartados(filtrado.dominiosDescartados())
            .duracionMs(duracion)
            .mensaje(String.format("Dominio descubierto: %s", dominio))
            .build();
    }

    /**
     * Devuelve la lista de empresas candidatas para descubrimiento masivo.
     * Lo usa el controller del endpoint masivo para iterar.
     */
    public List<Empresa> obtenerCandidatasPendientes(int limite) {
        return persistencia.buscarPendientesSinUrl(limite);
    }

    // ----- Helpers privados -----

    /**
     * Construye un ResultadoDescubrimiento con estado "error".
     * Centralizado para no repetir el builder en cada catch.
     */
    private ResultadoDescubrimiento construirError(Empresa empresa, String query,
                                                    String mensajeError, long inicio) {
        long duracion = System.currentTimeMillis() - inicio;
        return ResultadoDescubrimiento.builder()
            .empresaId(empresa.getId())
            .nombreEmpresa(empresa.getNombre())
            .estado(ResultadoDescubrimiento.Estado.error)
            .queryUsada(query)
            .duracionMs(duracion)
            .mensaje("Fallo técnico, no se ha tocado el estado de la empresa")
            .error(mensajeError)
            .build();
    }

    /**
     * Cuenta el total de resultados que devolvió Brave (antes de filtrar).
     * Maneja los nulls de la respuesta cruda con elegancia.
     */
    private int contarResultados(BraveSearchResponse respuesta) {
        if (respuesta == null
                || respuesta.getWeb() == null
                || respuesta.getWeb().getResults() == null) {
            return 0;
        }
        return respuesta.getWeb().getResults().size();
    }

    /**
     * Extrae el dominio (host) de una URL, normalizado: sin "www.", en minúsculas.
     * Replica la lógica de FiltroDominios.extraerDominio. Idealmente esto sería
     * un único helper compartido, pero por simplicidad lo duplicamos: dos métodos
     * de 10 líneas cada uno son aceptables.
     */
    private String extraerDominio(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null) {
                return null;
            }
            host = host.toLowerCase();
            if (host.startsWith("www.")) {
                host = host.substring(4);
            }
            return host;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * Mensaje legible para el caso "sin_resultado", explicando qué pasó.
     */
    private String construirMensajeSinResultado(int totalResultados,
                                                 List<String> dominiosDescartados) {
        if (totalResultados == 0) {
            return "Brave no devolvió ningún resultado para esta empresa";
        }
        return String.format(
            "Brave devolvió %d resultados pero ninguno superó el filtro de dominios "
                + "corporativos. Descartados: %s",
            totalResultados,
            String.join(", ", dominiosDescartados)
        );
    }
}