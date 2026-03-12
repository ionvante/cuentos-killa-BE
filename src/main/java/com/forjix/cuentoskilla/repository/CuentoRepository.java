package com.forjix.cuentoskilla.repository;

import com.forjix.cuentoskilla.model.Cuento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CuentoRepository extends JpaRepository<Cuento, Long> {
    List<Cuento> findByHabilitadoTrue();
    Page<Cuento> findByHabilitadoTrue(Pageable pageable);

    /**
     * RM-01: Búsqueda dinámica con filtros opcionales.
     * Todos los parámetros son opcionales; si vienen null se ignoran.
     */
    @Query("SELECT c FROM Cuento c WHERE c.habilitado = true " +
           "AND (:q IS NULL OR LOWER(c.titulo) LIKE CAST(:q AS string) " +
           "     OR LOWER(c.autor) LIKE CAST(:q AS string)) " +
           "AND (:categoria IS NULL OR c.categoria = :categoria) " +
           "AND (:edad IS NULL OR c.edadRecomendada = :edad) " +
           "AND (:precioMin IS NULL OR c.precio >= :precioMin) " +
           "AND (:precioMax IS NULL OR c.precio <= :precioMax)")
    Page<Cuento> buscarConFiltros(
        @Param("q") String q,
        @Param("categoria") String categoria,
        @Param("edad") String edad,
        @Param("precioMin") Double precioMin,
        @Param("precioMax") Double precioMax,
        Pageable pageable
    );
}
