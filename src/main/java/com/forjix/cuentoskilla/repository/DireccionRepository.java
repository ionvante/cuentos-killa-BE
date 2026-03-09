package com.forjix.cuentoskilla.repository;

import com.forjix.cuentoskilla.model.Direccion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DireccionRepository extends JpaRepository<Direccion, Long> {
    List<Direccion> findByUsuarioId(Long usuarioId);
    Optional<Direccion> findByIdAndUsuarioId(Long id, Long usuarioId);
}
