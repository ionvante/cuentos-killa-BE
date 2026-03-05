package com.forjix.cuentoskilla.repository;

import com.forjix.cuentoskilla.model.Maestro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaestroRepository extends JpaRepository<Maestro, Long> {
    List<Maestro> findByGrupoAndEstadoTrue(String grupo);

    List<Maestro> findByGrupo(String grupo);

    Optional<Maestro> findByCodigo(String codigo);
}
