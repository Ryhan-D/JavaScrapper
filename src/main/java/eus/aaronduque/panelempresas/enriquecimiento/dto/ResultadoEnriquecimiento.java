package eus.aaronduque.panelempresas.enriquecimiento.dto;

import eus.aaronduque.panelempresas.empresas.entity.EstadoEnriquecimiento;
import lombok.Builder;
import lombok.Data;

/**
 * Lo devuelve EnriquecimientoService y lo consumen los controllers
 */
@Data
@Builder
public class ResultadoEnriquecimiento {

    private Long empresaId;
    private String nombreEmpresa;
    private EstadoEnriquecimiento estadoFinal;

    @Builder.Default
    private boolean tieneUrl = true;

    @Builder.Default
    private int paginasDescubiertas = 0;

    @Builder.Default
    private int personasExtraidas = 0;

    @Builder.Default
    private int emailsExtraidos = 0;

    @Builder.Default
    private int telefonosExtraidos = 0;

    private long duracionMs;

    private String mensaje;

    private String error;
}