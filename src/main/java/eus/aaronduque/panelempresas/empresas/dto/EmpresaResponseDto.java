package eus.aaronduque.panelempresas.empresas.dto;

import eus.aaronduque.panelempresas.empresas.entity.Empresa;
import eus.aaronduque.panelempresas.empresas.entity.EstadoEnriquecimiento;
import eus.aaronduque.panelempresas.empresas.entity.TamanoEmpresa;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class EmpresaResponseDto {

    private Long id;
    private String nombre;
    private String cif;
    private String sector;
    private String provincia;
    private String municipio;
    private String direccion;

    // Datos de categorización (pueden estar vacíos hasta que se enriquezca)
    private TamanoEmpresa tamano;
    private BigDecimal tamanoConfianza;
    private String tamanoJustificacion;
    private Integer empleadosEstimados;

    private EstadoEnriquecimiento estadoEnriquecimiento;
    private String notas;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    /**
     * Convierte una entidad Empresa en su DTO de respuesta.
     * Patrón: el DTO conoce a la entidad, pero la entidad no conoce al DTO.
     */
    public static EmpresaResponseDto desde(Empresa empresa) {
        return EmpresaResponseDto.builder()
            .id(empresa.getId())
            .nombre(empresa.getNombre())
            .cif(empresa.getCif())
            .sector(empresa.getSector())
            .provincia(empresa.getProvincia())
            .municipio(empresa.getMunicipio())
            .direccion(empresa.getDireccion())
            .tamano(empresa.getTamano())
            .tamanoConfianza(empresa.getTamanoConfianza())
            .tamanoJustificacion(empresa.getTamanoJustificacion())
            .empleadosEstimados(empresa.getEmpleadosEstimados())
            .estadoEnriquecimiento(empresa.getEstadoEnriquecimiento())
            .notas(empresa.getNotas())
            .fechaCreacion(empresa.getFechaCreacion())
            .fechaActualizacion(empresa.getFechaActualizacion())
            .build();
    }
}