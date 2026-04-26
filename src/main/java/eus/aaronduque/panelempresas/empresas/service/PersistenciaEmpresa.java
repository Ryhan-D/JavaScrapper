package eus.aaronduque.panelempresas.empresas.service;

import eus.aaronduque.panelempresas.empresas.entity.Empresa;
import eus.aaronduque.panelempresas.empresas.repository.EmpresaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Encapsula operaciones de BD sobre Empresa que necesitan transacciones
 * AISLADAS del flujo padre.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PersistenciaEmpresa {

    private final EmpresaRepository empresaRepository;

    /**
     * Guarda una empresa en una transacción NUEVA e independiente.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Empresa guardarAislada(Empresa empresa) {
        return empresaRepository.save(empresa);
    }
}