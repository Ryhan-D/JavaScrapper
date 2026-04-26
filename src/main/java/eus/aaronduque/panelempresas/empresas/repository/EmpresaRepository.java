package eus.aaronduque.panelempresas.empresas.repository;


import eus.aaronduque.panelempresas.empresas.entity.Empresa;
import eus.aaronduque.panelempresas.empresas.entity.EstadoEnriquecimiento;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long>,
 JpaSpecificationExecutor<Empresa> {


     // Busca una empresa por su CIF.
    Optional<Empresa> findByCif(String cif);

    
     // Comprueba si existe una empresa con ese CIF.
    boolean existsByCif(String cif);

     // Lista de empresas por estadoEnriquecimiento.
    List<Empresa> findByEstadoEnriquecimiento(EstadoEnriquecimiento estado);
}