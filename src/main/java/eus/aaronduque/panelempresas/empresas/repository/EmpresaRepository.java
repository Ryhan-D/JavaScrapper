package eus.aaronduque.panelempresas.empresas.repository;


import eus.aaronduque.panelempresas.empresas.entity.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    /**
     * Busca una empresa por su CIF.
     */
    Optional<Empresa> findByCif(String cif);

    /**
     * Comprueba si existe una empresa con ese CIF.
     * Útil para validar duplicados antes de insertar.
     */
    boolean existsByCif(String cif);
}