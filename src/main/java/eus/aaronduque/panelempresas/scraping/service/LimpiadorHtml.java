package eus.aaronduque.panelempresas.scraping.service;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
@Slf4j
public class LimpiadorHtml {

    /**
     * Elementos que no aportan información de contacto/equipo y solo
     * añaden ruido o dificultan la lectura del LLM.
     */
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
        ".cookie-banner",
        ".cookies-notice",
        "#cookie-notice",
        "#cookie-consent",
        ".gdpr-banner",
        ".chat-widget",
        ".intercom-launcher",
        ".tawk-widget",
        ".breadcrumb",
        ".breadcrumbs"
    };

    // Dominios de redes sociales que reconocemos en los <a href>
    private static final String[] DOMINIOS_REDES = {
        "linkedin.com",
        "twitter.com",
        "x.com",
        "facebook.com",
        "instagram.com",
        "youtube.com"
    };

    /**
     * Limpia el HTML y devuelve texto plano para alimentar al LLM.
     */
    public String limpiar(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }

        Document documento = Jsoup.parse(html);

        // 1. Rescatar enlaces antes de eliminar nada (header/footer pueden
        //    tener iconos sociales que .text() perdería).
        String bloqueEnlaces = extraerEnlacesUtiles(documento);

        // 2. Eliminar ruido obvio.
        eliminarRuido(documento);

        // 3. Texto del body entero, compactado.
        String texto = documento.body() != null ? documento.body().text() : "";
        String textoLimpio = compactarEspacios(texto);

        // 4. Anteponer enlaces (al principio para que sobrevivan al truncado).
        String resultado = bloqueEnlaces.isEmpty()
            ? textoLimpio
            : bloqueEnlaces + "\n\n" + textoLimpio;

        log.debug("HTML limpiado: {} caracteres -> {} caracteres ({}% reducción), enlaces extraídos: {}",
            html.length(), resultado.length(),
            html.length() > 0 ? 100 - (resultado.length() * 100 / html.length()) : 0,
            bloqueEnlaces.isEmpty() ? "ninguno" : bloqueEnlaces.lines().count() - 2);

        return resultado;
    }

    /**
     * Recorre todos los <a href="..."> y extrae mailtos, teles y redes sociales.
     * Devuelve un bloque formateado o "" si no encuentra nada.
     */
    private String extraerEnlacesUtiles(Document documento) {
        Set<String> emails = new LinkedHashSet<>();
        Set<String> telefonos = new LinkedHashSet<>();
        Set<String> redes = new LinkedHashSet<>();

        Elements enlaces = documento.select("a[href]");
        for (Element enlace : enlaces) {
            String href = enlace.attr("href").trim();
            if (href.isBlank()) continue;

            String hrefLower = href.toLowerCase();

            if (hrefLower.startsWith("mailto:")) {
                String email = href.substring("mailto:".length()).trim();
                int interrogante = email.indexOf('?');
                if (interrogante > 0) email = email.substring(0, interrogante);
                if (!email.isBlank()) emails.add(email);

            } else if (hrefLower.startsWith("tel:")) {
                String tel = href.substring("tel:".length()).trim();
                if (!tel.isBlank()) telefonos.add(tel);

            } else if (esRedSocial(hrefLower)) {
                redes.add(href);
            }
        }

        if (emails.isEmpty() && telefonos.isEmpty() && redes.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[ENLACES DETECTADOS EN LA WEB]\n");
        if (!emails.isEmpty()) {
            sb.append("Emails: ").append(String.join(", ", emails)).append("\n");
        }
        if (!telefonos.isEmpty()) {
            sb.append("Telefonos: ").append(String.join(", ", telefonos)).append("\n");
        }
        if (!redes.isEmpty()) {
            sb.append("Redes sociales: ").append(String.join(", ", redes)).append("\n");
        }
        sb.append("[FIN ENLACES]");

        return sb.toString();
    }

    private boolean esRedSocial(String hrefLower) {
        for (String dominio : DOMINIOS_REDES) {
            if (hrefLower.contains(dominio)) {
                return true;
            }
        }
        return false;
    }

    private void eliminarRuido(Document documento) {
        for (String selector : SELECTORES_RUIDO) {
            documento.select(selector).remove();
        }
    }

    private String compactarEspacios(String texto) {
        return texto
            .replaceAll("\\s*\\n\\s*\\n\\s*", "\n\n")
            .replaceAll("[ \\t]+", " ")
            .trim();
    }
}