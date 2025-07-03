package com.forjix.cuentoskilla.repository;

import com.forjix.cuentoskilla.config.ConfigItem;
import com.forjix.cuentoskilla.config.ConfigItemId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConfigItemRepository extends JpaRepository<ConfigItem, ConfigItemId> {
    List<ConfigItem> findByCategoryIdOrderById2(Integer categoryId);
}
