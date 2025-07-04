package com.forjix.cuentoskilla.model.DTOs;

import java.util.List;

public class StatsDto {
    private List<Long> cuentos;
    private List<Long> pedidos;
    private List<Long> usuarios;

    public List<Long> getCuentos() {
        return cuentos;
    }

    public void setCuentos(List<Long> cuentos) {
        this.cuentos = cuentos;
    }

    public List<Long> getPedidos() {
        return pedidos;
    }

    public void setPedidos(List<Long> pedidos) {
        this.pedidos = pedidos;
    }

    public List<Long> getUsuarios() {
        return usuarios;
    }

    public void setUsuarios(List<Long> usuarios) {
        this.usuarios = usuarios;
    }
}
