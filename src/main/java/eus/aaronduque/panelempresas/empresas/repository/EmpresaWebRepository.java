package eus.aaronduque.panelempresas.empresas.repository;

import eus.aaronduque.panelempresas.empresas.entity.EmpresaWeb;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmpresaWebRepository extends JpaRepository<EmpresaWeb, Long> {

    /**
     * Lo usaremos para implementar el patron "upsert":
     * si existe, actualizamos esa fila; si no existe, creamos una nueva.
     */
    Optional<EmpresaWeb> findByEmpresaId(Long empresaId);
}