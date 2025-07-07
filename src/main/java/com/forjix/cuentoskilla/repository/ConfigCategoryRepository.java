package com.forjix.cuentoskilla.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.forjix.cuentoskilla.model.ConfigCategory;

import java.util.Optional;

public interface ConfigCategoryRepository extends JpaRepository<ConfigCategory, Integer> {
    boolean existsByCode(String code);
    Optional<ConfigCategory> findByCode(String code);
}
