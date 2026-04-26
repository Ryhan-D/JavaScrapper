package eus.aaronduque.panelempresas.descubrimiento.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para parsear la respuesta JSON de la API de Brave Search.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BraveSearchResponse {

    /** Bloque "web" de la respuesta. Puede ser null si Brave no devolvió resultados web. */
    private Web web;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Web {
        private List<Result> results;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        private String title;

        private String url;

        private String description;

        private String age;
    }
}