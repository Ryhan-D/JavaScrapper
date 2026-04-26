package eus.aaronduque.panelempresas.extraccion.dto;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class DatosExtraidos {

    @Builder.Default
    private List<String> emailsGenericos = new ArrayList<>();

    @Builder.Default
    private List<String> telefonos = new ArrayList<>();

    @Builder.Default
    private List<PersonaContacto> personas = new ArrayList<>();

    @Builder.Default
    private Map<String, String> redesSociales = new java.util.HashMap<>();

    private String descripcionBreve;

    @Data
    @Builder
    public static class PersonaContacto {
        private String nombre;
        private String cargo;
        private String email;
        private String telefono;
    }
}