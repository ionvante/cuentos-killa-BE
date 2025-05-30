package com.forjix.cuentoskilla.model.DTOs;

import java.math.BigDecimal;

public class PedidoItemDTO {
    private Long cuentoId;
    private int cantidad;
    private String tituloCuento;
    private BigDecimal precioUnitario;

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }
    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }
    public String getTituloCuento() {
        return tituloCuento;
    }
    public void setTituloCuento(String tituloCuento) {
        this.tituloCuento = tituloCuento;
    }
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
