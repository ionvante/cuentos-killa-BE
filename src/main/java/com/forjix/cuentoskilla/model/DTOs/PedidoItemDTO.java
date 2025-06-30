package com.forjix.cuentoskilla.model.DTOs;

import java.math.BigDecimal;

public class PedidoItemDTO {
    private Long cuentoId;
    private String nombreCuento;
    private String imagenUrl;
    private BigDecimal precioUnitario;
    private int cantidad;
    private BigDecimal subtotal;

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }
    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }
    public String getNombreCuento() {
        return nombreCuento;
    }
    public void setNombreCuento(String nombreCuento) {
        this.nombreCuento = nombreCuento;
    }
    public String getImagenUrl() {
        return imagenUrl;
    }
    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
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
    public BigDecimal getSubtotal() {
        return subtotal;
    }
    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public PedidoItemDTO(Long cuentoId, int cantidad, String nombreCuento, BigDecimal precioUnitario, String imagenUrl, BigDecimal subtotal) {
        this.cuentoId = cuentoId;
        this.nombreCuento = nombreCuento;
        this.imagenUrl = imagenUrl;
        this.precioUnitario = precioUnitario;
        this.cantidad = cantidad;
        this.subtotal = subtotal;
    }

    public PedidoItemDTO() {
        // Constructor por defecto
    }
}
