package com.forjix.cuentoskilla.repository;

import com.forjix.cuentoskilla.model.Boleta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BoletaRepository extends JpaRepository<Boleta, Long> {
    Optional<Boleta> findByOrder_Id(Long orderId);
}

