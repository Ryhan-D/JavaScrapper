package eus.aaronduque.panelempresas.empresas.repository;

import eus.aaronduque.panelempresas.empresas.entity.Empresa;
import eus.aaronduque.panelempresas.empresas.entity.EstadoEnriquecimiento;
import eus.aaronduque.panelempresas.empresas.entity.TamanoEmpresa;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filtros reutilizables para queries dinámicas sobre Empresa.
 */
public class EmpresaEspecificaciones {

    /**
     * Filtra por provincia exacta. Si provincia es null, no aplica filtro.
     */
    public static Specification<Empresa> conProvincia(String provincia) {
        return (root, query, cb) -> {
            if (provincia == null || provincia.isBlank()) return cb.conjunction();
            return cb.equal(root.get("provincia"), provincia);
        };
    }

    /**
     * Filtra por tamaño de empresa (micro, pequena, mediana, grande).
     */
    public static Specification<Empresa> conTamano(TamanoEmpresa tamano) {
        return (root, query, cb) -> {
            if (tamano == null) return cb.conjunction();
            return cb.equal(root.get("tamano"), tamano);
        };
    }

    /**
     * Filtra por estado de enriquecimiento (pendiente, enriquecida...).
     */
    public static Specification<Empresa> conEstado(EstadoEnriquecimiento estado) {
        return (root, query, cb) -> {
            if (estado == null) return cb.conjunction();
            return cb.equal(root.get("estadoEnriquecimiento"), estado);
        };
    }

    /**
     * Búsqueda parcial por nombre (case-insensitive, sin importar acentos).
     */
    public static Specification<Empresa> conNombreParecidoA(String texto) {
        return (root, query, cb) -> {
            if (texto == null || texto.isBlank()) return cb.conjunction();
            String patron = "%" + texto.toLowerCase().trim() + "%";
            return cb.like(cb.lower(root.get("nombre")), patron);
        };
    }

    /**
     * Filtra por sector exacto.
     */
    public static Specification<Empresa> conSector(String sector) {
        return (root, query, cb) -> {
            if (sector == null || sector.isBlank()) return cb.conjunction();
            return cb.equal(root.get("sector"), sector);
        };
    }
}