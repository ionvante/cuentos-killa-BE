package com.forjix.cuentoskilla.model;

import jakarta.persistence.*;

@Entity
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Cuento cuento;

    @ManyToOne
    private User user;

    private int cantidad;

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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    // Getters y setters
}
