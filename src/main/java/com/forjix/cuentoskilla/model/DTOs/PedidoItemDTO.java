package com.forjix.cuentoskilla.model.DTOs;

public class PedidoItemDTO {
    private Long cuentoId;
    private int cantidad;
    public Long getCuentoId() {
        return cuentoId;
    }
    public void setCuentoId(Long cuentoId) {
        this.cuentoId = cuentoId;
    }
    public int getCantidad() {
        return cantidad;
    }
    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }
}
