package com.forjix.cuentoskilla.model.DTOs;

import java.util.List;

public class PedidoDTO {
    private Long Id;
    private Long id;
    private String fecha;
    private String nombre;
    private String correo;
    private String direccion;
    private String telefono;
    private List<PedidoItemDTO> items;
    private double total;
    private String estado;
    private String tipoPago;
    private Long userId;
    private String correoUsuario;

    // Getters y setters
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getIdCapital() {
        return Id;
    }
    public void setIdCapital(Long Id) {
        this.Id = Id;
    }
    public String getFecha() {
        return fecha;
    }
    public void setFecha(String fecha) {
        this.fecha = fecha;
    }
    public String getNombre() {
        return nombre;
    }
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    public String getCorreo() {
        return correo;
    }
    public void setCorreo(String correo) {
        this.correo = correo;
    }
    public String getDireccion() {
        return direccion;
    }
    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }
    public String getTelefono() {
        return telefono;
    }
    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }
    public List<PedidoItemDTO> getItems() {
        return items;
    }
    public void setItems(List<PedidoItemDTO> items) {
        this.items = items;
    }
    public double getTotal() {
        return total;
    }
    public void setTotal(double total) {
        this.total = total;
    }
    public String getEstado() {
        return estado;
    }
    public void setEstado(String estado) {
        this.estado = estado;
    }
    public String getTipoPago() {
        return tipoPago;
    }
    public void setTipoPago(String tipoPago) {
        this.tipoPago = tipoPago;
    }

    public Long getUserId() {
        return userId;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
    }  

    public String getCorreoUsuario() {
        return correoUsuario;
    }
    public void setCorreoUsuario(String correoUsuario) {
        this.correoUsuario = correoUsuario;
    }
}
