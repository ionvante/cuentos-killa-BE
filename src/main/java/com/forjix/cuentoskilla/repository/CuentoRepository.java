package com.forjix.cuentoskilla.repository;

import com.forjix.cuentoskilla.model.Cuento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CuentoRepository extends JpaRepository<Cuento, Long> {
}
