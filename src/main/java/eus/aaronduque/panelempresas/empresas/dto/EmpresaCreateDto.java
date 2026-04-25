package eus.aaronduque.panelempresas.empresas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EmpresaCreateDto {

    @NotBlank(message = "El nombre de la empresa es obligatorio")
    @Size(max = 255, message = "El nombre no puede exceder 255 caracteres")
    private String nombre;

    @Size(max = 12, message = "El CIF no puede exceder 12 caracteres")
    @Pattern(
        regexp = "^[A-Z]\\d{8}$|^[A-Z]\\d{7}[A-Z0-9]$|^$",
        message = "Formato de CIF inválido"
    )
    private String cif;

    @Size(max = 100)
    private String sector;

    @Pattern(
        regexp = "^(Bizkaia|Gipuzkoa|Araba|Navarra)?$",
        message = "Provincia debe ser Bizkaia, Gipuzkoa, Araba o Navarra"
    )
    private String provincia;

    @Size(max = 100)
    private String municipio;

    private String direccion;

    private String notas;
}