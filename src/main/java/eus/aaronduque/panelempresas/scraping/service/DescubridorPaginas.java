package eus.aaronduque.panelempresas.scraping.service;

import eus.aaronduque.panelempresas.scraping.dto.ResultadoScraping;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class DescubridorPaginas {

    private final ScraperService scraperService;

    /**
     * Rutas típicas donde las empresas suelen tener info de contacto.
     * El orden importa: las más probables primero.
     */
    private static final String[] RUTAS_CANDIDATAS = {
        "/",
        "/contacto",
        "/contact",
        "/contact-us",
        "/contactanos",
        "/contacto/",
        "/contact/",
        "/equipo",
        "/team",
        "/nuestro-equipo",
        "/sobre-nosotros",
        "/about",
        "/about-us",
        "/quienes-somos",
        "/miembros",
        "/trabajadores",
        "/plantilla",
        "/empresa"
    };

    public List<ResultadoScraping> descubrir(String dominioRaiz, int maxPaginas) {
    String urlBase = normalizarUrl(dominioRaiz);
    List<ResultadoScraping> exitosas = new ArrayList<>();
    Set<String> hashesVistos = new HashSet<>();  // para detectar contenido duplicado

    log.info("Descubriendo páginas en {}", urlBase);

    for (String ruta : RUTAS_CANDIDATAS) {
        if (exitosas.size() >= maxPaginas) {
            log.debug("Límite de {} páginas alcanzado, parando descubrimiento", maxPaginas);
            break;
        }

        String urlCompleta = urlBase + ruta;
        ResultadoScraping resultado = scraperService.descargar(urlCompleta);

        if (!resultado.isExitoso() || resultado.getHtml() == null 
            || resultado.getHtml().length() <= 1000) {
            try { Thread.sleep(500); } catch (InterruptedException e) { 
                Thread.currentThread().interrupt(); break; 
            }
            continue;
        }

        // Detectar contenido duplicado por longitud + hash
        String firma = resultado.getHtml().length() + "_" + resultado.getHtml().hashCode();
        if (hashesVistos.contains(firma)) {
            log.debug("Saltando {} (mismo contenido ya descargado)", urlCompleta);
            try { Thread.sleep(500); } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); break;
            }
            continue;
        }
        hashesVistos.add(firma);
        exitosas.add(resultado);

        try { Thread.sleep(500); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
        }
    }

    log.info("Descubrimiento completado: {} páginas exitosas únicas", exitosas.size());
    return exitosas;
}

    /**
     * Normaliza la URL: quita la barra final y asegura https.
     */
    private String normalizarUrl(String url) {
        String normalizada = url.trim();
        if (normalizada.endsWith("/")) {
            normalizada = normalizada.substring(0, normalizada.length() - 1);
        }
        if (!normalizada.startsWith("http")) {
            normalizada = "https://" + normalizada;
        }
        return normalizada;
    }
}