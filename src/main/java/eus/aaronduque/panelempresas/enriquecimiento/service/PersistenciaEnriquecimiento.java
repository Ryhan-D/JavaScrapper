package eus.aaronduque.panelempresas.enriquecimiento.service;

import eus.aaronduque.panelempresas.empresas.entity.Empresa;
import eus.aaronduque.panelempresas.empresas.entity.EmpresaContacto;
import eus.aaronduque.panelempresas.empresas.entity.EmpresaWeb;
import eus.aaronduque.panelempresas.empresas.entity.EstadoEnriquecimiento;
import eus.aaronduque.panelempresas.empresas.repository.EmpresaContactoRepository;
import eus.aaronduque.panelempresas.empresas.repository.EmpresaRepository;
import eus.aaronduque.panelempresas.empresas.repository.EmpresaWebRepository;
import eus.aaronduque.panelempresas.extraccion.dto.DatosExtraidos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsula las operaciones de BD del enriquecimiento.
 * 
 * Esta en un servicio separado de EnriquecimientoService para que las
 * anotaciones @Transactional funcionen correctamente: Spring solo aplica
 * la transacción cuando la llamada cruza el proxy (de un bean a otro).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PersistenciaEnriquecimiento {

    private final EmpresaRepository empresaRepository;
    private final EmpresaWebRepository empresaWebRepository;
    private final EmpresaContactoRepository empresaContactoRepository;

    /**
     * Cambia el estado de la empresa y lo persiste.
     */
    @Transactional
    public void marcarEstado(Empresa empresa, EstadoEnriquecimiento nuevoEstado) {
        empresa.setEstadoEnriquecimiento(nuevoEstado);
        empresaRepository.save(empresa);
        log.debug("Empresa {} → estado {}", empresa.getId(), nuevoEstado);
    }

    /**
     * Guarda el resultado completo de un enriquecimiento exitoso
     * Todo en UNA transacción para que sea atomica
     */
    @Transactional
    public void guardarExito(Empresa empresa, EmpresaWeb webExistente, String url, DatosExtraidos datos) {
        // 1. Upsert EmpresaWeb
        EmpresaWeb web = webExistente != null ? webExistente : new EmpresaWeb();
        web.setEmpresa(empresa);
        web.setUrlCompleta(url);
        web.setDominio(extraerentity(url));
        web.setEmailsGenericos(new ArrayList<>(datos.getEmailsGenericos()));
        web.setTelefonos(new ArrayList<>(datos.getTelefonos()));
        web.setRedesSociales(datos.getRedesSociales());
        web.setDescripcionExtraida(datos.getDescripcionBreve());
        web.setUltimoScraping(LocalDateTime.now());
        web.setScrapingExitoso(true);
        web.setScrapingError(null);
        empresaWebRepository.save(web);

        // 2. Borrar contactos previos
        int borrados = empresaContactoRepository.deleteByEmpresaId(empresa.getId());
        if (borrados > 0) {
            log.info("Empresa {}: borrados {} contactos previos", empresa.getId(), borrados);
        }

        // 3. Insertar contactos nuevos
        List<EmpresaContacto> nuevos = datos.getPersonas().stream()
            .filter(p -> p.getNombre() != null && !p.getNombre().isBlank())
            .map(p -> EmpresaContacto.builder()
                .empresa(empresa)
                .nombre(p.getNombre())
                .cargo(p.getCargo())
                .email(p.getEmail())
                .telefono(p.getTelefono())
                .fuente("scraping_llm")
                .build())
            .toList();

        if (!nuevos.isEmpty()) {
            empresaContactoRepository.saveAll(nuevos);
            log.info("Empresa {}: insertados {} contactos nuevos", empresa.getId(), nuevos.size());
        }
    }

    /**
     * Guarda el rastro de un enriquecimiento fallido en EmpresaWeb
     */
    @Transactional
    public void guardarFallo(Empresa empresa, EmpresaWeb webExistente, String url,
                             String mensajeError, boolean scrapingExitoso) {
        EmpresaWeb web = webExistente != null ? webExistente : new EmpresaWeb();
        web.setEmpresa(empresa);
        web.setUrlCompleta(url);
        web.setDominio(extraerentity(url));
        web.setUltimoScraping(LocalDateTime.now());
        web.setScrapingExitoso(scrapingExitoso);
        web.setScrapingError(mensajeError);
        empresaWebRepository.save(web);

        // Marcar la empresa como error
        empresa.setEstadoEnriquecimiento(EstadoEnriquecimiento.error);
        empresaRepository.save(empresa);
    }

    /**
     * Extrae el entity (sin protocolo, sin path) de una URL
     */
    private String extraerentity(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            String sinProtocolo = url.replaceFirst("^https?://", "");
            String soloHost = sinProtocolo.split("/")[0];
            return soloHost.startsWith("www.") ? soloHost.substring(4) : soloHost;
        } catch (Exception e) {
            return url;
        }
    }
}