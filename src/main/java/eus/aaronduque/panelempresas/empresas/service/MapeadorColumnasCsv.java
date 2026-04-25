package eus.aaronduque.panelempresas.empresas.service;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mapea nombres de columnas variables de CSVs externos 
 * Maneja acentos, mayúsculas/minúsculas y sinónimos comunes.
 */
public class MapeadorColumnasCsv {

    
    private static final Map<String, Set<String>> SINONIMOS = Map.of(
        "nombre",    Set.of("nombre", "razon social", "razon_social", "denominacion", "empresa", "razon"),
        "cif",       Set.of("cif", "nif", "identificacion fiscal", "identificacion_fiscal", "codigo fiscal"),
        "sector",    Set.of("sector", "actividad", "cnae descripcion", "cnae_descripcion", "rama"),
        "provincia", Set.of("provincia", "prov"),
        "municipio", Set.of("municipio", "ciudad", "localidad", "poblacion"),
        "direccion", Set.of("direccion", "domicilio", "ubicacion"),
        "notas",     Set.of("notas", "observaciones", "comentarios")
    );

    /**
     * Dado el header del CSV, devuelve un mapa: campo canónico -> nombre real en el CSV.
     */
    public static Map<String, String> mapear(List<String> headerCsv) {
        Map<String, String> resultado = new HashMap<>();

        for (String columnaCsv : headerCsv) {
            String normalizada = normalizar(columnaCsv);

            for (Map.Entry<String, Set<String>> entrada : SINONIMOS.entrySet()) {
                if (entrada.getValue().contains(normalizada)) {
                    resultado.put(entrada.getKey(), columnaCsv);
                    break;
                }
            }
        }

        return resultado;
    }

    /**
     * Normaliza un texto, sin acentos, mayusculas
     */
    private static String normalizar(String texto) {
        if (texto == null) return "";
        String sinAcentos = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return sinAcentos.toLowerCase();
    }
}