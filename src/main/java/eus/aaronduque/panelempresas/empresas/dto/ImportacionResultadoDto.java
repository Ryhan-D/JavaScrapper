package eus.aaronduque.panelempresas.empresas.dto;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class ImportacionResultadoDto {

    private int totalFilas;
    private int importadas;
    private int saltadasDuplicadas;
    private int saltadasError;

    @Builder.Default
    private List<ErrorFila> errores = new ArrayList<>();

    @Data
    @Builder
    public static class ErrorFila {
        private int linea;
        private String mensaje;
    }
}