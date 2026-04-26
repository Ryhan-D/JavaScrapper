package eus.aaronduque.panelempresas.descubrimiento.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Resultado de intentar descubrir el dominio oficial de una empresa
 */
@Data
@Builder
public class ResultadoDescubrimiento {

    /** Estados posibles del descubrimiento. */
    public enum Estado {
        encontrado,
        sin_resultado,
        error
    }

    private Long empresaId;
    private String nombreEmpresa;
    private Estado estado;

    private String queryUsada;

    private String dominioEncontrado;

    private String urlCompletaEncontrada;

    @Builder.Default
    private int resultadosBrave = 0;

    @Builder.Default
    private int resultadosTrasFiltro = 0;

    @Builder.Default
    private List<String> dominiosDescartados = List.of();

    private long duracionMs;

    private String mensaje;

    private String error;
}