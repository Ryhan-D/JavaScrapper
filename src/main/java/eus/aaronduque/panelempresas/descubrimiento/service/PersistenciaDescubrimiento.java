package eus.aaronduque.panelempresas.descubrimiento.service;

import eus.aaronduque.panelempresas.empresas.entity.Empresa;
import eus.aaronduque.panelempresas.empresas.entity.EmpresaWeb;
import eus.aaronduque.panelempresas.empresas.entity.EstadoEnriquecimiento;
import eus.aaronduque.panelempresas.empresas.repository.EmpresaRepository;
import eus.aaronduque.panelempresas.empresas.repository.EmpresaWebRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Encapsula las operaciones de BD del descubrimiento de dominios.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PersistenciaDescubrimiento {

    private final EmpresaRepository empresaRepository;
    private final EmpresaWebRepository empresaWebRepository;

   
     // Guarda el dominio descubierto en empresas_web.

    @Transactional
    public void guardarDominio(Empresa empresa, String dominio, String urlCompleta) {
        EmpresaWeb web = empresaWebRepository.findByEmpresaId(empresa.getId())
            .orElseGet(() -> EmpresaWeb.builder()
                .empresa(empresa)
                .build());

        web.setDominio(dominio);
        web.setUrlCompleta(urlCompleta);
        // No tocamos scrapingExitoso/scrapingError aquí: los gestiona el
        // flujo de enriquecimiento, no el de descubrimiento.

        empresaWebRepository.save(web);

        log.info("Dominio guardado para empresa {} ({}): {}",
            empresa.getId(), empresa.getNombre(), dominio);
    }

    /**
     * Marca una empresa como sin_web cuando Brave no devolvió ningún dominio
     * que sobreviva al filtro.
     */
    @Transactional
    public void marcarSinWeb(Empresa empresa, String queryUsada, List<String> dominiosDescartados) {
        // 1. Cambiar estado de la empresa
        empresa.setEstadoEnriquecimiento(EstadoEnriquecimiento.sin_web);
        empresaRepository.save(empresa);

        // 2. Guardar trazabilidad en empresas_web
        String mensajeError = construirMensajeError(queryUsada, dominiosDescartados);

        EmpresaWeb web = empresaWebRepository.findByEmpresaId(empresa.getId())
            .orElseGet(() -> EmpresaWeb.builder()
                .empresa(empresa)
                .build());

        web.setScrapingExitoso(false);
        web.setScrapingError(mensajeError);
        web.setUltimoScraping(LocalDateTime.now());
        // No tocamos dominio/urlCompleta: si por alguna razón ya había uno
        // (descubrimiento anterior parcial), lo dejamos por trazabilidad.

        empresaWebRepository.save(web);

        log.info("Empresa {} ({}) marcada sin_web. Descartados: {}",
            empresa.getId(), empresa.getNombre(), dominiosDescartados);
    }

    /**
     * Devuelve empresas candidatas para descubrimiento masivo
     */
    public List<Empresa> buscarPendientesSinUrl(int limite) {
        return empresaRepository.buscarPendientesSinUrl(PageRequest.of(0, limite));
    }

    /**
     * Construye un mensaje de error humano-legible que documenta por qué
     * Brave no encontró un dominio para esta empresa. Se guarda en
     * empresas_web.scraping_error para trazabilidad.
     */
    private String construirMensajeError(String queryUsada, List<String> dominiosDescartados) {
        if (dominiosDescartados == null || dominiosDescartados.isEmpty()) {
            return "Brave no devolvió resultados para query: '" + queryUsada + "'";
        }
        return String.format(
            "Brave no devolvió ningún dominio corporativo para query: '%s'. "
                + "Descartados por filtro: %s",
            queryUsada,
            String.join(", ", dominiosDescartados)
        );
    }
}