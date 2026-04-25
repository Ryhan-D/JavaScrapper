package eus.aaronduque.panelempresas.empresas.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BusquedaEmpresasDto {

    private List<EmpresaResponseDto> contenido;
    private int paginaActual;
    private int tamanoPagina;
    private long totalElementos;
    private int totalPaginas;
}