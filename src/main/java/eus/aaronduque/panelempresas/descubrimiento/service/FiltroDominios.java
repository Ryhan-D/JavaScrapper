package eus.aaronduque.panelempresas.descubrimiento.service;

import eus.aaronduque.panelempresas.descubrimiento.dto.BraveSearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Decide cuál es el dominio oficial de una empresa entre los resultados
 * que devuelve Brave Search.
 */
@Service
@Slf4j
public class FiltroDominios {

    /**
     * Resultado del filtrado: el resultado elegido (si lo hay) y la lista
     * de dominios que fueron descartados por el camino
     */
    public record ResultadoFiltro(
        Optional<BraveSearchResponse.Result> elegido,
        List<String> dominiosDescartados
    ) {}

    /**
     * Lista negra de dominios. Cualquier resultado de Brave cuyo host
     * (sin "www.") este en esta lista se descarta.
     */
    private static final Set<String> LISTA_NEGRA = Set.of(
        // Redes sociales y plataformas
        "linkedin.com",
        "facebook.com",
        "twitter.com",
        "x.com",
        "instagram.com",
        "youtube.com",
        "tiktok.com",
        "pinterest.com",

        // Agregadores de empresas (España)
        "einforma.com",
        "axesor.es",
        "axesor.com",
        "empresia.es",
        "infoempresa.com",
        "datospublicos.es",
        "expansion.com",
        "eleconomista.es",
        "dnb.com",
        "kompass.com",
        "europages.es",

        // Prensa generalista
        "elcorreo.com",
        "elpais.com",
        "elmundo.es",
        "abc.es",
        "lavanguardia.com",
        "eldiario.es",
        "20minutos.es",

        // Prensa regional vasca
        "deia.eus",
        "naiz.eus",
        "noticiasdegipuzkoa.eus",
        "noticiasdealava.eus",
        "diariovasco.com",

        // Portales institucionales (genéricos, no oficiales de empresa)
        "bizkaia.eus",
        "gipuzkoa.eus",
        "araba.eus",
        "euskadi.eus",
        "gob.es",
        "boe.es",
        "borme.es",

        // Portales de empleo
        "infojobs.net",
        "indeed.com",
        "glassdoor.com",
        "glassdoor.es",
        "tecnoempleo.com",
        "ticjob.es",

        // Wikis y enciclopedias
        "wikipedia.org",
        "wikidata.org"
    );

    /**
     * Filtra los resultados de Brave y devuelve el primer dominio oficial
     */
    public ResultadoFiltro filtrar(BraveSearchResponse respuesta) {
        // Defensa: respuesta vacía o sin bloque "web"
        if (respuesta == null || respuesta.getWeb() == null
                || respuesta.getWeb().getResults() == null
                || respuesta.getWeb().getResults().isEmpty()) {
            log.debug("Brave devolvió una respuesta sin resultados web");
            return new ResultadoFiltro(Optional.empty(), List.of());
        }

        List<BraveSearchResponse.Result> resultados = respuesta.getWeb().getResults();
        java.util.List<String> descartados = new java.util.ArrayList<>();

        for (BraveSearchResponse.Result resultado : resultados) {
            String dominio = extraerDominio(resultado.getUrl());

            if (dominio == null) {
                log.warn("URL no parseable, se ignora: {}", resultado.getUrl());
                continue;
            }

            if (esDominioNoCorporativo(dominio)) {
                descartados.add(dominio);
                log.debug("Descartado por lista negra: {}", dominio);
                continue;
            }

            // Primer superviviente: ese es el dominio oficial
            log.info("Dominio elegido: {} (descartados previos: {})",
                dominio, descartados);
            return new ResultadoFiltro(Optional.of(resultado), descartados);
        }

        // Llegamos al final sin encontrar nada que pase el filtro
        log.info("Ningún resultado sobrevivió al filtro. Descartados: {}", descartados);
        return new ResultadoFiltro(Optional.empty(), descartados);
    }

    /**
     * Extrae el dominio (host) de una URL completa, normalizado
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
     * Comprueba si un dominio está en la lista negra.
     */
    private boolean esDominioNoCorporativo(String dominio) {
        // Coincidencia exacta
        if (LISTA_NEGRA.contains(dominio)) {
            return true;
        }
        // Coincidencia por sufijo
        for (String prohibido : LISTA_NEGRA) {
            if (dominio.endsWith("." + prohibido)) {
                return true;
            }
        }
        return false;
    }
}