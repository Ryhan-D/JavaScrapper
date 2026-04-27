package eus.aaronduque.panelempresas.enriquecimiento.service;

import eus.aaronduque.panelempresas.empresas.entity.Empresa;
import eus.aaronduque.panelempresas.empresas.entity.EmpresaWeb;
import eus.aaronduque.panelempresas.empresas.entity.EstadoEnriquecimiento;
import eus.aaronduque.panelempresas.empresas.repository.EmpresaRepository;
import eus.aaronduque.panelempresas.empresas.repository.EmpresaWebRepository;
import eus.aaronduque.panelempresas.enriquecimiento.dto.ResultadoEnriquecimiento;
import eus.aaronduque.panelempresas.extraccion.dto.DatosExtraidos;
import eus.aaronduque.panelempresas.extraccion.service.ExtractorLLM;
import eus.aaronduque.panelempresas.scraping.dto.ResultadoScraping;
import eus.aaronduque.panelempresas.scraping.service.DescubridorPaginas;
import eus.aaronduque.panelempresas.scraping.service.LimpiadorHtml;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnriquecimientoService {

    // Repositorios (solo lectura — las escrituras viven en PersistenciaEnriquecimiento)
    private final EmpresaRepository empresaRepository;
    private final EmpresaWebRepository empresaWebRepository;

    // Servicios de scraping y extracción
    private final DescubridorPaginas descubridorPaginas;
    private final LimpiadorHtml limpiadorHtml;
    private final ExtractorLLM extractorLLM;

    // Persistencia transaccional (bean separado para que el proxy de Spring funcione)
    private final PersistenciaEnriquecimiento persistencia;

    // Configuración
    private static final int MAX_PAGINAS_POR_EMPRESA = 5;

    /**
     * Enriquece una empresa: scraping + extracción LLM + guardado en BD.
     */
    public ResultadoEnriquecimiento enriquecer(Long empresaId) {
        long inicio = System.currentTimeMillis();
        log.info("=== Iniciando enriquecimiento de empresa {} ===", empresaId);

        // 1. Cargar empresa
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Empresa no encontrada: " + empresaId));

        String nombreEmpresa = empresa.getNombre();

        // 2. Comprobar URL
        Optional<EmpresaWeb> webExistente = empresaWebRepository.findByEmpresaId(empresaId);
        String url = webExistente
                .map(EmpresaWeb::getUrlCompleta)
                .filter(u -> u != null && !u.isBlank())
                .orElse(null);

        if (url == null) {
            log.warn("Empresa {} ({}) no tiene URL en empresas_web", empresaId, nombreEmpresa);
            persistencia.marcarEstado(empresa, EstadoEnriquecimiento.sin_web);
            return ResultadoEnriquecimiento.builder()
                    .empresaId(empresaId)
                    .nombreEmpresa(nombreEmpresa)
                    .estadoFinal(EstadoEnriquecimiento.sin_web)
                    .tieneUrl(false)
                    .duracionMs(System.currentTimeMillis() - inicio)
                    .mensaje("La empresa no tiene URL configurada en empresas_web")
                    .build();
        }

        // 3. Marcar como procesando
        persistencia.marcarEstado(empresa, EstadoEnriquecimiento.procesando);

        try {
            // 4. Descubrir y descargar páginas
            log.info("Descubriendo páginas para {} (URL: {})", nombreEmpresa, url);
            List<ResultadoScraping> paginas = descubridorPaginas.descubrir(url, MAX_PAGINAS_POR_EMPRESA);

            if (paginas.isEmpty()) {
                String msg = "No se descubrió ninguna página accesible";
                log.warn("Empresa {}: {}", empresaId, msg);
                persistencia.guardarFallo(empresa, webExistente.orElse(null), url, msg, false);
                return resultadoError(empresaId, nombreEmpresa, inicio, msg, 0);
            }

            // 5. Limpiar HTML y llamar a Gemini
            String textoCombinado = paginas.stream()
                    .map(p -> "--- " + p.getUrl() + " ---\n" + limpiadorHtml.limpiar(p.getHtml()))
                    .collect(Collectors.joining("\n\n"));

            log.info("Empresa {}: {} páginas, {} caracteres totales tras limpieza",
                    empresaId, paginas.size(), textoCombinado.length());

            DatosExtraidos datos = extractorLLM.extraer(textoCombinado);

            // 6. Guardar resultado
            persistencia.guardarExito(empresa, webExistente.orElse(null), url, datos);

            // 7. Marcar como enriquecida
            persistencia.marcarEstado(empresa, EstadoEnriquecimiento.enriquecida);

            long duracion = System.currentTimeMillis() - inicio;
            log.info("=== Empresa {} enriquecida en {}ms: {} personas, {} emails, {} teléfonos ===",
                    empresaId, duracion,
                    datos.getPersonas().size(),
                    datos.getEmailsGenericos().size(),
                    datos.getTelefonos().size());

            return ResultadoEnriquecimiento.builder()
                    .empresaId(empresaId)
                    .nombreEmpresa(nombreEmpresa)
                    .estadoFinal(EstadoEnriquecimiento.enriquecida)
                    .tieneUrl(true)
                    .paginasDescubiertas(paginas.size())
                    .personasExtraidas(datos.getPersonas().size())
                    .emailsExtraidos(datos.getEmailsGenericos().size())
                    .telefonosExtraidos(datos.getTelefonos().size())
                    .duracionMs(duracion)
                    .mensaje("Enriquecimiento completado con éxito")
                    .build();

        } catch (Exception e) {
            String msg = "Error inesperado durante enriquecimiento: " + e.getMessage();
            log.error("Empresa {}: {}", empresaId, msg, e);
            persistencia.guardarFallo(empresa, webExistente.orElse(null), url, msg, true);
            return resultadoError(empresaId, nombreEmpresa, inicio, msg, 0);
        }
    }


    // ============================================================
    // METODOS PRIVADOS auxiliares (sin acceso a BD)
    // ============================================================

    /**
     * Construye un ResultadoEnriquecimiento de error
     */
    private ResultadoEnriquecimiento resultadoError(Long empresaId, String nombreEmpresa,
                                                     long inicio, String mensajeError,
                                                     int paginasDescubiertas) {
        return ResultadoEnriquecimiento.builder()
            .empresaId(empresaId)
            .nombreEmpresa(nombreEmpresa)
            .estadoFinal(EstadoEnriquecimiento.error)
            .tieneUrl(true)
            .paginasDescubiertas(paginasDescubiertas)
            .duracionMs(System.currentTimeMillis() - inicio)
            .mensaje("Enriquecimiento fallido")
            .error(mensajeError)
            .build();
    }
}