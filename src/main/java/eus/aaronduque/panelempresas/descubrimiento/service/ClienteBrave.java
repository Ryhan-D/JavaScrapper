package eus.aaronduque.panelempresas.descubrimiento.service;

import eus.aaronduque.panelempresas.descubrimiento.dto.BraveSearchResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.Optional;

/**
 * Cliente HTTP para la API de Brave Search.
 */
@Service
@Slf4j
public class ClienteBrave {

    private final RestClient restClient;
    private final String urlBase;
    private final int resultadosPorBusqueda;
    private final String pais;
    private final String idioma;

    public ClienteBrave(
        @Value("${brave.api-key}") String apiKey,
        @Value("${brave.url-base}") String urlBase,
        @Value("${brave.timeout-segundos}") int timeoutSegundos,
        @Value("${brave.resultados-por-busqueda}") int resultadosPorBusqueda,
        @Value("${brave.pais}") String pais,
        @Value("${brave.idioma}") String idioma
    ) {
        this.urlBase = urlBase;
        this.resultadosPorBusqueda = resultadosPorBusqueda;
        this.pais = pais;
        this.idioma = idioma;

        // Configuramos timeouts a nivel de petición HTTP. Sin esto, una API
        // colgada bloquearía hilos del servidor indefinidamente.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(timeoutSegundos).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(timeoutSegundos).toMillis());

        // El RestClient se construye una vez y se reutiliza para todas las peticiones.
        this.restClient = RestClient.builder()
            .requestFactory(factory)
            .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("X-Subscription-Token", apiKey)
            .build();
    }

    
    // No tira la app: solo loggea. Brave petará en runtime con 401 si se llega a usar.
     
    @PostConstruct
    void verificarConfiguracion() {
        log.info("ClienteBrave configurado: urlBase={}, count={}, country={}, lang={}",
            urlBase, resultadosPorBusqueda, pais, idioma);
    }


   // Lanza una busqueda en Brave con la query indicada.

    public Optional<BraveSearchResponse> buscar(String query) {
        if (query == null || query.isBlank()) {
            log.warn("Query vacía, no se llama a Brave");
            return Optional.empty();
        }

        // Construimos la URL con UriComponentsBuilder para que codifique
        // bien los espacios, acentos y caracteres especiales del nombre.
        String url = UriComponentsBuilder.fromUriString(urlBase)
            .queryParam("q", query)
            .queryParam("count", resultadosPorBusqueda)
            .queryParam("country", pais)
            .queryParam("search_lang", idioma)
            .build()
            .toUriString();

        log.debug("Llamando a Brave: query='{}'", query);

        try {
            BraveSearchResponse respuesta = restClient.get()
                .uri(url)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    // Loggeamos pero NO lanzamos excepción
                    log.error("Brave devolvió HTTP {}: {}",
                        res.getStatusCode(), res.getStatusText());
                })
                .body(BraveSearchResponse.class);

            if (respuesta == null) {
                log.warn("Brave devolvió cuerpo vacío para query: {}", query);
                return Optional.empty();
            }

            int total = (respuesta.getWeb() != null && respuesta.getWeb().getResults() != null)
                ? respuesta.getWeb().getResults().size()
                : 0;
            log.debug("Brave devolvió {} resultados para query='{}'", total, query);

            return Optional.of(respuesta);

        } catch (RestClientException e) {
            // Cubre timeouts, fallos de red, JSON malformado, etc.
            log.error("Error llamando a Brave Search: {}", e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            // Defensa frente a cualquier otra cosa rara
            log.error("Error inesperado llamando a Brave Search: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
}