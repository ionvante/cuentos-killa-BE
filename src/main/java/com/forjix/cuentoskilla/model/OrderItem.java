package com.forjix.cuentoskilla.model;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_item")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cuento_id")
    private Cuento cuento;

    @ManyToOne
    @JoinColumn(name = "order_id")
    @JsonBackReference
    private Order order;

    private int cantidad;
    @Column(name = "precio_unitario")
    private double precioUnitario;
    private String nombre;
    @Column(name = "imagen_url")
    private String imagenUrl;
    private BigDecimal subtotal;


    // Getters y setters
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Cuento getCuento() {
        return cuento;
    }
    public void setCuento(Cuento cuento) {
        this.cuento = cuento;
    }
    public Order getOrder() {
        return order;
    }
    public void setOrder(Order order) {
        this.order = order;
    }
    public int getCantidad() {
        return cantidad;
    }
    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }
    public double getPrecio_unitario() {
        return precioUnitario;
    }
    public void setPrecio_unitario(double precioUnitario) {
        this.precioUnitario = precioUnitario;
    }
    public String getNombre() {
        return nombre;
    }
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    public String getImagen_url() {
        return imagenUrl;
    }
    public void setImagen_url(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }
    public BigDecimal getSubtotal() {
        return subtotal;
    }
    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
}
