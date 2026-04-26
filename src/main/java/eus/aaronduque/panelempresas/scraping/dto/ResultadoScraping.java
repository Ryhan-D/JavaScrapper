package eus.aaronduque.panelempresas.scraping.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResultadoScraping {

    private String url;
    private String html;
    private boolean exitoso;
    private String mensajeError;
    private int codigoRespuesta;

    /**
     * Resultado de exito
     */
    public static ResultadoScraping exito(String url, String html, int codigoRespuesta) {
        return ResultadoScraping.builder()
            .url(url)
            .html(html)
            .exitoso(true)
            .codigoRespuesta(codigoRespuesta)
            .build();
    }

    /**
     * Resultado de fallo. Mantiene la URL para saber que fallo
     */
    public static ResultadoScraping error(String url, String mensajeError) {
        return ResultadoScraping.builder()
            .url(url)
            .exitoso(false)
            .mensajeError(mensajeError)
            .build();
    }
}