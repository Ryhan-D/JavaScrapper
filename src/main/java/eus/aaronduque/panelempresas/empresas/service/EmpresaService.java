package eus.aaronduque.panelempresas.empresas.service;

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
                "Ya existe una empresa con el CIF " + empresa.getCif()
            );
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
}