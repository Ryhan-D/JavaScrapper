package eus.aaronduque.panelempresas.empresas.service;

import eus.aaronduque.panelempresas.empresas.dto.ImportacionResultadoDto;
import eus.aaronduque.panelempresas.empresas.dto.ImportacionResultadoDto.ErrorFila;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import eus.aaronduque.panelempresas.empresas.dto.BusquedaEmpresasDto;
import eus.aaronduque.panelempresas.empresas.dto.EmpresaResponseDto;
import eus.aaronduque.panelempresas.empresas.entity.TamanoEmpresa;
import eus.aaronduque.panelempresas.empresas.repository.EmpresaEspecificaciones;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

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

    public boolean esDuplicada(Empresa empresa) {

        return empresa.getCif() != null
                && empresaRepository.existsByCif(empresa.getCif());
    }

    @Transactional
    public Empresa crear(Empresa empresa) {
        // si trae CIF, comprobar que no este duplicado
        if (esDuplicada(empresa)) {
            throw new IllegalArgumentException(
                    "Ya existe una empresa con el CIF " + empresa.getCif());
        }

        empresa.setEstadoEnriquecimiento(EstadoEnriquecimiento.pendiente);

        Empresa guardada = empresaRepository.save(empresa);
        log.info("Empresa creada con id {} y nombre '{}'", guardada.getId(), guardada.getNombre());
        return guardada;
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
                int linea = (int) registro.getRecordNumber() + 1;

                try {
                    Empresa empresa = construirEmpresaDesdeRegistro(registro, mapeo);

                    // nombre obligatorio
                    if (empresa.getNombre() == null || empresa.getNombre().isBlank()) {
                        errores.add(ErrorFila.builder().linea(linea).mensaje("Nombre vacío").build());
                        saltadasError++;
                        continue;
                    }

                    // Comprobar duplicado por CIF
                    if (esDuplicada(empresa)) {
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
     * Construye una Empresa a partir de un registro CSV usando el mapeo de columnas
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
     * Lee el valor de una columna del CSV de forma segura
     */
    private String valorSeguro(CSVRecord registro, String nombreColumna) {
        if (nombreColumna == null)
            return null;
        String valor = registro.get(nombreColumna);
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }

    /**
     * Busca empresas con filtros opcionales y paginacion.
     */
    @Transactional(readOnly = true)
    public BusquedaEmpresasDto buscar(
            String nombre,
            String provincia,
            TamanoEmpresa tamano,
            EstadoEnriquecimiento estado,
            String sector,
            Pageable pageable) {

        // Combinamos las especificaciones que esten activas
        Specification<Empresa> filtros = Specification
                .allOf(
                        EmpresaEspecificaciones.conNombreParecidoA(nombre),
                        EmpresaEspecificaciones.conProvincia(provincia),
                        EmpresaEspecificaciones.conTamano(tamano),
                        EmpresaEspecificaciones.conEstado(estado),
                        EmpresaEspecificaciones.conSector(sector));

        Page<Empresa> pagina = empresaRepository.findAll(filtros, pageable);

        List<EmpresaResponseDto> contenido = pagina.getContent()
                .stream()
                .map(EmpresaResponseDto::desde)
                .toList();

        return BusquedaEmpresasDto.builder()
                .contenido(contenido)
                .paginaActual(pagina.getNumber())
                .tamanoPagina(pagina.getSize())
                .totalElementos(pagina.getTotalElements())
                .totalPaginas(pagina.getTotalPages())
                .build();
    }
}