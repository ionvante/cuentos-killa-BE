package com.forjix.cuentoskilla.repository;

import com.forjix.cuentoskilla.model.Maestro;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaestroRepository extends JpaRepository<Maestro, Long> {
    List<Maestro> findByGrupoAndEstadoTrue(String grupo);

    List<Maestro> findByGrupo(String grupo);

    Optional<Maestro> findByCodigo(String codigo);

    @Query("SELECT m FROM Maestro m WHERE m.grupo = :grupo AND m.codigo IN :codigos")
    List<Maestro> findByGrupoAndCodigoIn(@Param("grupo") String grupo, @Param("codigos") List<String> codigos);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Maestro m WHERE m.codigo = :codigo")
    Optional<Maestro> findByCodigoForUpdate(@Param("codigo") String codigo);
}

