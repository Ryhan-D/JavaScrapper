package eus.aaronduque.panelempresas.compartido.excepciones;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class ErrorRespuestaDto {

    private int estado;
    private String mensaje;

    @Builder.Default
    private List<ErrorCampo> errores = new ArrayList<>();

    @Builder.Default
    private LocalDateTime fecha = LocalDateTime.now();

    @Data
    @Builder
    public static class ErrorCampo {
        private String campo;
        private String mensaje;
    }
}