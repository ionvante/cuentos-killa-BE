package com.forjix.cuentoskilla.repository;

import com.forjix.cuentoskilla.model.Distrito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DistritoRepository extends JpaRepository<Distrito, String> {
    List<Distrito> findByProvinciaId(String provinciaId);
}
