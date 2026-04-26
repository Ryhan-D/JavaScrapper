package eus.aaronduque.panelempresas.empresas.repository;

import eus.aaronduque.panelempresas.empresas.entity.Empresa;
import eus.aaronduque.panelempresas.empresas.entity.EstadoEnriquecimiento;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long>,
        JpaSpecificationExecutor<Empresa> {

    // Busca una empresa por su CIF
    Optional<Empresa> findByCif(String cif);

    // Comprueba si existe una empresa con ese CIF
    boolean existsByCif(String cif);

    // Lista de empresas por estadoEnriquecimiento
    List<Empresa> findByEstadoEnriquecimiento(EstadoEnriquecimiento estado);

    /**
     * Empresas candidatas para descubrimiento de dominio
     * El LIMIT se aplica vía Pageable: la persistencia llama a este método
     * con PageRequest.of(0, limite).
     */
    @Query("""
        SELECT e FROM Empresa e
        LEFT JOIN EmpresaWeb w ON w.empresa = e
        WHERE e.estadoEnriquecimiento = eus.aaronduque.panelempresas.empresas.entity.EstadoEnriquecimiento.pendiente
          AND (w IS NULL OR w.urlCompleta IS NULL OR w.urlCompleta = '')
        ORDER BY e.id ASC
        """)
    List<Empresa> buscarPendientesSinUrl(Pageable pageable);
}