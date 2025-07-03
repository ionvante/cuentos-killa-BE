package com.forjix.cuentoskilla.config;

import java.io.Serializable;
import java.util.Objects;

public class ConfigItemId implements Serializable {
    private Integer categoryId;
    private Integer id2;

    public ConfigItemId() {
    }

    public ConfigItemId(Integer categoryId, Integer id2) {
        this.categoryId = categoryId;
        this.id2 = id2;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public Integer getId2() {
        return id2;
    }

    public void setId2(Integer id2) {
        this.id2 = id2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConfigItemId that = (ConfigItemId) o;
        return Objects.equals(categoryId, that.categoryId) && Objects.equals(id2, that.id2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(categoryId, id2);
    }
}
