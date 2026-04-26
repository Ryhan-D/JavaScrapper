package eus.aaronduque.panelempresas.empresas.repository;

import eus.aaronduque.panelempresas.empresas.entity.EmpresaContacto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmpresaContactoRepository extends JpaRepository<EmpresaContacto, Long> {

    /**
     * Devuelve todos los contactos asociados a una empresa
     */
    List<EmpresaContacto> findByEmpresaId(Long empresaId);

    /**
     * Borra de golpe todos los contactos de una empresa
     * Lo usamos al re-enriquecer: borrar viejos antes de insertar los nuevos
     */
    @Modifying
    @Query("DELETE FROM EmpresaContacto c WHERE c.empresa.id = :empresaId")
    int deleteByEmpresaId(@Param("empresaId") Long empresaId);
}