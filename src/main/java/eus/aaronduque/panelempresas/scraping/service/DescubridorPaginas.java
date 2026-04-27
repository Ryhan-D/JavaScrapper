package eus.aaronduque.panelempresas.scraping.service;

import eus.aaronduque.panelempresas.scraping.dto.ResultadoScraping;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Descubre las páginas relevantes de una web corporativa para extraer
 * información de contacto y equipo.

 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DescubridorPaginas {

    private final ScraperService scraperService;

    /**
     * Rutas típicamente usadas para páginas de contacto.
     */
    private static final List<String> RUTAS_CONTACTO = List.of(
        "/contacto",
        "/contactanos",
        "/contacto/",
        "/contact",
        "/contact/",
        "/contact-us"
    );

    /**
     * Rutas típicamente usadas para páginas de equipo o "sobre nosotros".
     */
    private static final List<String> RUTAS_EQUIPO_SOBRE_NOSOTROS = List.of(
        "/equipo",
        "/nuestro-equipo",
        "/team",
        "/quienes-somos",
        "/sobre-nosotros",
        "/about",
        "/about-us",
        "/empresa",
        "/miembros",
        "/trabajadores",
        "/plantilla"
    );

    /**
     * Pausa entre descargas para no saturar la web objetivo (cortesía).
     */
    private static final long PAUSA_ENTRE_DESCARGAS_MS = 500;

    public List<ResultadoScraping> descubrir(String dominioRaiz, int maxPaginas) {
        String urlBase = normalizarUrl(dominioRaiz);
        List<ResultadoScraping> exitosas = new ArrayList<>();
        Set<String> firmasVistas = new HashSet<>();

        log.info("Descubriendo páginas en {}", urlBase);

        // 1. Home: siempre primero. Si falla, igual da, seguimos.
        intentarRuta(urlBase, "/", exitosas, firmasVistas, maxPaginas);

        // 2. Contacto: solo nos quedamos con la primera válida que aporte
        //    contenido nuevo (no duplicada de home).
        boolean contactoEncontrado = false;
        for (String ruta : RUTAS_CONTACTO) {
            if (exitosas.size() >= maxPaginas) break;
            if (intentarRuta(urlBase, ruta, exitosas, firmasVistas, maxPaginas)) {
                log.info("Categoría contacto: encontrada en {}", ruta);
                contactoEncontrado = true;
                break;
            }
        }
        if (!contactoEncontrado) {
            log.debug("Categoría contacto: ninguna ruta válida (puede que el contacto esté en la home)");
        }

        // 3. Equipo + sobre-nosotros: aquí seguimos hasta llenar el cupo.
        int paginasEquipoEncontradas = 0;
        for (String ruta : RUTAS_EQUIPO_SOBRE_NOSOTROS) {
            if (exitosas.size() >= maxPaginas) break;
            if (intentarRuta(urlBase, ruta, exitosas, firmasVistas, maxPaginas)) {
                paginasEquipoEncontradas++;
            }
        }
        log.info("Categoría equipo/sobre-nosotros: {} páginas únicas encontradas", paginasEquipoEncontradas);

        log.info("Descubrimiento completado: {} páginas únicas en {}", exitosas.size(), urlBase);
        return exitosas;
    }

    /**
     * Intenta descargar una ruta y añadirla a la lista si es válida y no duplicada.
     */
    private boolean intentarRuta(String urlBase, String ruta,
                                  List<ResultadoScraping> exitosas,
                                  Set<String> firmasVistas,
                                  int maxPaginas) {
        if (exitosas.size() >= maxPaginas) return false;

        String urlCompleta = urlBase + ruta;
        ResultadoScraping resultado = scraperService.descargar(urlCompleta);

        // Pausa de cortesía siempre (descargada con éxito o no).
        pausa();

        if (!resultado.isExitoso()
                || resultado.getHtml() == null
                || resultado.getHtml().length() <= 1000) {
            return false;
        }

        String firma = calcularFirmaContenido(resultado.getHtml());
        if (!firmasVistas.add(firma)) {
            log.debug("Saltando {} (contenido duplicado de una página ya descargada)", urlCompleta);
            return false;
        }

        exitosas.add(resultado);
        return true;
    }

    /**
     * Calcula una firma del contenido visible de la página, no del HTML crudo.
     */
    private String calcularFirmaContenido(String html) {
        Document doc = Jsoup.parse(html);
        String texto = doc.body() != null ? doc.body().text() : "";
        String normalizado = texto.toLowerCase().replaceAll("\\s+", " ").trim();
        return normalizado.hashCode() + "_" + normalizado.length();
    }

    /**
     * Pausa de cortesía entre descargas. Si nos interrumpen, restauramos el
     * flag de interrupción y dejamos al llamador decidir.
     */
    private void pausa() {
        try {
            Thread.sleep(PAUSA_ENTRE_DESCARGAS_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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