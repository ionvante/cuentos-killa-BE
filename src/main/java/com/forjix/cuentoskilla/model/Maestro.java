package com.forjix.cuentoskilla.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "tabla_catalogo_general")
public class Maestro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "grupo_codigo", nullable = false, length = 50)
    private String grupo;

    @Column(name = "codigo_maestro", nullable = false, length = 50, unique = true)
    private String codigo;

    @Column(name = "valor_mostrar", nullable = false, length = 150)
    private String valor;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Column(name = "estado", nullable = false)
    private Boolean estado = true;

    // Constructores
    public Maestro() {
    }

    public Maestro(String grupo, String codigo, String valor, String descripcion, Boolean estado) {
        this.grupo = grupo;
        this.codigo = codigo;
        this.valor = valor;
        this.descripcion = descripcion;
        this.estado = estado;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGrupo() {
        return grupo;
    }

    public void setGrupo(String grupo) {
        this.grupo = grupo;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Boolean getEstado() {
        return estado;
    }

    public void setEstado(Boolean estado) {
        this.estado = estado;
    }

    @Override
    public String toString() {
        return "Maestro{" +
                "id=" + id +
                ", grupo='" + grupo + '\'' +
                ", codigo='" + codigo + '\'' +
                ", valor='" + valor + '\'' +
                ", descripcion='" + descripcion + '\'' +
                ", estado=" + estado +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Maestro maestro = (Maestro) o;

        return id != null ? id.equals(maestro.id) : maestro.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
