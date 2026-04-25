package eus.aaronduque.panelempresas.empresas.controller;

import eus.aaronduque.panelempresas.empresas.dto.EmpresaCreateDto;
import eus.aaronduque.panelempresas.empresas.dto.EmpresaResponseDto;
import eus.aaronduque.panelempresas.empresas.entity.Empresa;
import eus.aaronduque.panelempresas.empresas.service.EmpresaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
     * Lista todas las empresas.
     */
    @GetMapping
    public ResponseEntity<List<EmpresaResponseDto>> listar() {
        List<EmpresaResponseDto> empresas = empresaService.listarTodas()
            .stream()
            .map(EmpresaResponseDto::desde)
            .toList();

        return ResponseEntity.ok(empresas);
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
}