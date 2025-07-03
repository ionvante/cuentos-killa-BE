package com.forjix.cuentoskilla.repository;

import com.forjix.cuentoskilla.config.ConfigCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfigCategoryRepository extends JpaRepository<ConfigCategory, Integer> {
    boolean existsByCode(String code);
    Optional<ConfigCategory> findByCode(String code);
}
