package eus.aaronduque.panelempresas.scraping.service;

import eus.aaronduque.panelempresas.scraping.dto.ResultadoScraping;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;

@Service
@Slf4j
public class ScraperService {

    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    
    // User-Agent realista para no parecer un bot de bajo nivel
    private static final String USER_AGENT = 
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    /**
     * Descarga el HTML de una URL.
     * Captura todos los errores comunes y los convierte en ResultadoScraping.error.
     */
    public ResultadoScraping descargar(String url) {
        log.debug("Descargando HTML de {}", url);

        try {
            Connection.Response respuesta = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout((int) TIMEOUT.toMillis())
                .followRedirects(true)
                .ignoreHttpErrors(false)
                .execute();

            Document documento = respuesta.parse();
            String html = documento.outerHtml();

            log.info("✓ Descarga exitosa de {} ({} bytes, código {})",
                url, html.length(), respuesta.statusCode());

            return ResultadoScraping.exito(url, html, respuesta.statusCode());

        } catch (HttpStatusException e) {
            log.warn("✗ Error HTTP {} al descargar {}", e.getStatusCode(), url);
            return ResultadoScraping.error(url,
                "HTTP " + e.getStatusCode() + " al acceder a " + url);

        } catch (SocketTimeoutException e) {
            log.warn("✗ Timeout al descargar {}", url);
            return ResultadoScraping.error(url, "Timeout tras " + TIMEOUT.getSeconds() + "s");

        } catch (IOException e) {
            log.warn("✗ Error de red al descargar {}: {}", url, e.getMessage());
            return ResultadoScraping.error(url, "Error de red: " + e.getMessage());

        } catch (Exception e) {
            log.error("✗ Error inesperado al descargar {}", url, e);
            return ResultadoScraping.error(url, "Error inesperado: " + e.getMessage());
        }
    }
}