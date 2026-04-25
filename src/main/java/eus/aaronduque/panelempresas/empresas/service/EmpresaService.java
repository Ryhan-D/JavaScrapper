package eus.aaronduque.panelempresas.empresas.service;

import eus.aaronduque.panelempresas.empresas.dto.ImportacionResultadoDto;
import eus.aaronduque.panelempresas.empresas.dto.ImportacionResultadoDto.ErrorFila;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;

import eus.aaronduque.panelempresas.empresas.entity.Empresa;
import eus.aaronduque.panelempresas.empresas.entity.EstadoEnriquecimiento;
import eus.aaronduque.panelempresas.empresas.repository.EmpresaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmpresaService {

    private final EmpresaRepository empresaRepository;

    /**
     * Crea una nueva empresa en la base de datos.
     */
    @Transactional
    public Empresa crear(Empresa empresa) {
        // si trae CIF, comprobar que no este duplicado
        if (empresa.getCif() != null && empresaRepository.existsByCif(empresa.getCif())) {
            throw new IllegalArgumentException(
                    "Ya existe una empresa con el CIF " + empresa.getCif());
        }

        empresa.setEstadoEnriquecimiento(EstadoEnriquecimiento.pendiente);

        Empresa guardada = empresaRepository.save(empresa);
        log.info("Empresa creada con id {} y nombre '{}'", guardada.getId(), guardada.getNombre());
        return guardada;
    }

    /**
     * Devuelve todas las empresas. Sin paginacion de momento
     */
    @Transactional(readOnly = true)
    public List<Empresa> listarTodas() {
        return empresaRepository.findAll();
    }

    /**
     * Busca una empresa por su id.
     */
    @Transactional(readOnly = true)
    public Optional<Empresa> buscarPorId(Long id) {
        return empresaRepository.findById(id);
    }

    /**
     * Importa empresas desde un InputStream con formato CSV.
     * - Mapea columnas con sinónimos.
     * - Valida cada fila individualmente.
     * - Salta duplicados por CIF sin abortar.
     * - Devuelve estadísticas de la importación.
     */
    @Transactional
    public ImportacionResultadoDto importarDesdeCsv(InputStream csvStream) throws IOException {
        int totalFilas = 0;
        int importadas = 0;
        int saltadasDuplicadas = 0;
        int saltadasError = 0;
        List<ErrorFila> errores = new ArrayList<>();

        try (InputStreamReader reader = new InputStreamReader(csvStream, StandardCharsets.UTF_8);
                CSVParser parser = new CSVParser(reader,
                        CSVFormat.DEFAULT.builder()
                                .setHeader()
                                .setSkipHeaderRecord(true)
                                .setIgnoreEmptyLines(true)
                                .setTrim(true)
                                .build())) {

            // Mapear el header del CSV a nuestros campos canónicos
            List<String> headerOriginal = parser.getHeaderNames();
            Map<String, String> mapeo = MapeadorColumnasCsv.mapear(headerOriginal);

            if (!mapeo.containsKey("nombre")) {
                throw new IllegalArgumentException(
                        "El CSV no contiene una columna reconocible para 'nombre'. " +
                                "Aceptamos: nombre, razon social, denominacion, empresa.");
            }

            log.info("Mapeo de columnas detectado: {}", mapeo);

            for (CSVRecord registro : parser) {
                totalFilas++;
                int linea = (int) registro.getRecordNumber() + 1; // +1 porque la cabecera es la 1

                try {
                    Empresa empresa = construirEmpresaDesdeRegistro(registro, mapeo);

                    // Validación mínima: nombre obligatorio
                    if (empresa.getNombre() == null || empresa.getNombre().isBlank()) {
                        errores.add(ErrorFila.builder().linea(linea).mensaje("Nombre vacío").build());
                        saltadasError++;
                        continue;
                    }

                    // Comprobar duplicado por CIF
                    if (empresa.getCif() != null && !empresa.getCif().isBlank()
                            && empresaRepository.existsByCif(empresa.getCif())) {
                        saltadasDuplicadas++;
                        continue;
                    }

                    empresa.setEstadoEnriquecimiento(EstadoEnriquecimiento.pendiente);
                    empresaRepository.save(empresa);
                    importadas++;

                } catch (Exception e) {
                    errores.add(ErrorFila.builder().linea(linea).mensaje(e.getMessage()).build());
                    saltadasError++;
                }
            }
        }

        log.info("Importación finalizada: {} importadas, {} duplicadas, {} con error",
                importadas, saltadasDuplicadas, saltadasError);

        return ImportacionResultadoDto.builder()
                .totalFilas(totalFilas)
                .importadas(importadas)
                .saltadasDuplicadas(saltadasDuplicadas)
                .saltadasError(saltadasError)
                .errores(errores)
                .build();
    }

    /**
     * Construye una Empresa a partir de un registro CSV usando el mapeo de
     * columnas.
     */
    private Empresa construirEmpresaDesdeRegistro(CSVRecord registro, Map<String, String> mapeo) {
        Empresa empresa = new Empresa();
        empresa.setNombre(valorSeguro(registro, mapeo.get("nombre")));
        empresa.setCif(valorSeguro(registro, mapeo.get("cif")));
        empresa.setSector(valorSeguro(registro, mapeo.get("sector")));
        empresa.setProvincia(valorSeguro(registro, mapeo.get("provincia")));
        empresa.setMunicipio(valorSeguro(registro, mapeo.get("municipio")));
        empresa.setDireccion(valorSeguro(registro, mapeo.get("direccion")));
        empresa.setNotas(valorSeguro(registro, mapeo.get("notas")));
        return empresa;
    }

    /**
     * Lee el valor de una columna del CSV de forma segura:
     * - Si la columna no estaba mapeada (null), devuelve null.
     * - Si está vacía, devuelve null (mejor para la BD que cadena vacía).
     */
    private String valorSeguro(CSVRecord registro, String nombreColumna) {
        if (nombreColumna == null)
            return null;
        String valor = registro.get(nombreColumna);
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }

}