package com.forjix.cuentoskilla.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "cuento")
public class Cuento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String titulo;
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getTitulo() {
        return titulo;
    }
    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }
    public String getAutor() {
        return autor;
    }
    public void setAutor(String autor) {
        this.autor = autor;
    }
    public String getDescripcionCorta() {
        return descripcionCorta;
    }
    public void setDescripcionCorta(String descripcionCorta) {
        this.descripcionCorta = descripcionCorta;
    }
    public String getEditorial() {
        return editorial;
    }
    public void setEditorial(String editorial) {
        this.editorial = editorial;
    }
    public String getTipoEdicion() {
        return tipoEdicion;
    }
    public void setTipoEdicion(String tipoEdicion) {
        this.tipoEdicion = tipoEdicion;
    }
    public int getNroPaginas() {
        return nroPaginas;
    }
    public void setNroPaginas(int nroPaginas) {
        this.nroPaginas = nroPaginas;
    }
    public LocalDate getFechaPublicacion() {
        return fechaPublicacion;
    }
    public void setFechaPublicacion(LocalDate fechaPublicacion) {
        this.fechaPublicacion = fechaPublicacion;
    }
    public LocalDate getFechaIngreso() {
        return fechaIngreso;
    }
    public void setFechaIngreso(LocalDate fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }
    public String getEdadRecomendada() {
        return edadRecomendada;
    }
    public void setEdadRecomendada(String edadRecomendada) {
        this.edadRecomendada = edadRecomendada;
    }
    public int getStock() {
        return stock;
    }
    public void setStock(int stock) {
        this.stock = stock;
    }
    private String autor;
    private String descripcionCorta;
    private String editorial;
    private String tipoEdicion;
    private int nroPaginas;
    private LocalDate fechaPublicacion;
    private LocalDate fechaIngreso;
    private String edadRecomendada;
    private int stock;

    // Getters y setters
}
