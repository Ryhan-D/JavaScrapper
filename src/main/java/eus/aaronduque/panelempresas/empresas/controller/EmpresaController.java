package eus.aaronduque.panelempresas.empresas.controller;

import eus.aaronduque.panelempresas.empresas.dto.BusquedaEmpresasDto;
import eus.aaronduque.panelempresas.empresas.dto.EmpresaCreateDto;
import eus.aaronduque.panelempresas.empresas.dto.EmpresaResponseDto;
import eus.aaronduque.panelempresas.empresas.dto.ImportacionResultadoDto;
import eus.aaronduque.panelempresas.empresas.entity.Empresa;
import eus.aaronduque.panelempresas.empresas.entity.EstadoEnriquecimiento;
import eus.aaronduque.panelempresas.empresas.entity.TamanoEmpresa;
import eus.aaronduque.panelempresas.empresas.service.EmpresaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/empresas")
@RequiredArgsConstructor
public class EmpresaController {

    private final EmpresaService empresaService;

    /**
     * POST /api/empresas
     * Crea una nueva empresa a partir del JSON enviado.
     */
    @PostMapping
    public ResponseEntity<EmpresaResponseDto> crear(@Valid @RequestBody EmpresaCreateDto dto) {
        Empresa empresa = new Empresa();
        empresa.setNombre(dto.getNombre());
        empresa.setCif(dto.getCif());
        empresa.setSector(dto.getSector());
        empresa.setProvincia(dto.getProvincia());
        empresa.setMunicipio(dto.getMunicipio());
        empresa.setDireccion(dto.getDireccion());
        empresa.setNotas(dto.getNotas());

        Empresa creada = empresaService.crear(empresa);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(EmpresaResponseDto.desde(creada));
    }

    /**
     * GET /api/empresas
     * Lista empresas con filtros opcionales y paginación.
     * Sin parámetros equivale a "listar todas las empresas paginadas".
     */
    @GetMapping
    public ResponseEntity<BusquedaEmpresasDto> buscar(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String provincia,
            @RequestParam(required = false) TamanoEmpresa tamano,
            @RequestParam(required = false) EstadoEnriquecimiento estado,
            @RequestParam(required = false) String sector,
            @PageableDefault(size = 20, sort = "fechaCreacion") Pageable pageable) {

        BusquedaEmpresasDto resultado = empresaService.buscar(
                nombre, provincia, tamano, estado, sector, pageable);
        return ResponseEntity.ok(resultado);
    }

    /**
     * GET /api/empresas/{id}
     * Obtiene una empresa concreta por su id.
     */
    @GetMapping("/{id}")
    public ResponseEntity<EmpresaResponseDto> obtenerPorId(@PathVariable Long id) {
        return empresaService.buscarPorId(id)
                .map(EmpresaResponseDto::desde)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/empresas/importar
     * Importa un archivo CSV con empresas.
     */
    @PostMapping("/importar")
    public ResponseEntity<ImportacionResultadoDto> importarCsv(
            @RequestParam("archivo") MultipartFile archivo) throws IOException {

        if (archivo.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        if (!esCsv(archivo)) {
            return ResponseEntity.badRequest().build();
        }

        ImportacionResultadoDto resultado = empresaService.importarDesdeCsv(archivo.getInputStream());
        return ResponseEntity.ok(resultado);
    }

    private boolean esCsv(MultipartFile archivo) {
        String nombre = archivo.getOriginalFilename();
        String contentType = archivo.getContentType();
        return (nombre != null && nombre.toLowerCase().endsWith(".csv"))
                || "text/csv".equalsIgnoreCase(contentType);
    }
}