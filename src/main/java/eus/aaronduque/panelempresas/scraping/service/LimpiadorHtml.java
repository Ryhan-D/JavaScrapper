package eus.aaronduque.panelempresas.scraping.service;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class LimpiadorHtml {

    // Elementos que casi nunca contienen info útil de contacto
private static final String[] SELECTORES_RUIDO = {
    "script",
    "style",
    "noscript",
    "iframe",
    "svg",
    "img",
    "video",
    "audio",
    "canvas",
    // Solo headers/navs específicos, no wildcards
    "header.site-header",
    "header.main-header",
    "header#header",
    "nav.main-nav",
    "nav.primary-nav",
    "nav.navbar",
    ".main-menu",
    ".primary-menu",
    ".site-navigation",
    "#main-menu",
    "#primary-menu",
    // Cookies
    ".cookie-banner",
    ".cookies-notice",
    "#cookie-notice",
    "#cookie-consent",
    ".gdpr-banner",
    // Chats y widgets
    ".chat-widget",
    ".intercom-launcher",
    ".tawk-widget",
    // Migas de pan
    ".breadcrumb",
    ".breadcrumbs"
};

    // Selectores de secciones que SÍ suelen contener contacto/equipo
    private static final String[] SELECTORES_INTERESANTES = {
        "section:contains(Contacto)",
        "section:contains(Equipo)",
        "section:contains(Nosotros)",
        "section:contains(Quienes)",
        "div[id*=contact]",
        "div[id*=equipo]",
        "div[class*=contact]",
        "div[class*=team]",
        "div[class*=equipo]",
        "footer",
        "address"
    };

    /**
     * Limpia el HTML y extrae solo el texto relevante para extracción de contactos.
     * Devuelve texto plano (sin HTML), priorizando secciones de contacto/equipo.
     */
    public String limpiar(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }

        Document documento = Jsoup.parse(html);
        eliminarRuido(documento);

        // Intentar extraer solo secciones relevantes
        String textoEnfocado = extraerSeccionesInteresantes(documento);

        // Si las secciones interesantes nos dan poco, devolver el body completo
        if (textoEnfocado.length() < 500) {
            log.debug("Pocas secciones interesantes encontradas, usando body completo");
            textoEnfocado = documento.body() != null ? documento.body().text() : "";
        }

        String resultado = compactarEspacios(textoEnfocado);
        log.debug("HTML limpiado: {} caracteres -> {} caracteres ({}% reducción)",
            html.length(), resultado.length(),
            html.length() > 0 ? 100 - (resultado.length() * 100 / html.length()) : 0);

        return resultado;
    }

    /**
     * Elimina del documento todos los nodos que sabemos que no aportan info.
     */
    private void eliminarRuido(Document documento) {
        for (String selector : SELECTORES_RUIDO) {
            documento.select(selector).remove();
        }
    }

    /**
     * Busca secciones que típicamente contienen contacto/equipo y devuelve su texto.
     */
    private String extraerSeccionesInteresantes(Document documento) {
        List<String> trozos = new ArrayList<>();

        for (String selector : SELECTORES_INTERESANTES) {
            try {
                Elements elementos = documento.select(selector);
                for (Element elemento : elementos) {
                    String texto = elemento.text().trim();
                    if (!texto.isBlank() && texto.length() > 20) {
                        trozos.add(texto);
                    }
                }
            } catch (Exception e) {
                // Algunos selectores con :contains() pueden lanzar excepciones
                // si el documento es raro. Las ignoramos.
            }
        }

        return String.join("\n\n", trozos);
    }

    /**
     * Compacta espacios en blanco múltiples (saltos, tabs, espacios) en un solo espacio.
     * Mantiene los párrafos separados por doble salto de línea.
     */
    private String compactarEspacios(String texto) {
        return texto
            .replaceAll("\\s*\\n\\s*\\n\\s*", "\n\n")  // saltos múltiples → doble salto
            .replaceAll("[ \\t]+", " ")                 // espacios/tabs múltiples → uno
            .trim();
    }
}