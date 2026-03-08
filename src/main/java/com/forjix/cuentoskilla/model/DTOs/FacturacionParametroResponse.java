package com.forjix.cuentoskilla.model.DTOs;

public class FacturacionParametroResponse {
    private String codigo;
    private String valor;
    private String descripcion;
    private Boolean estado;

    public FacturacionParametroResponse() {
    }

    public FacturacionParametroResponse(String codigo, String valor, String descripcion, Boolean estado) {
        this.codigo = codigo;
        this.valor = valor;
        this.descripcion = descripcion;
        this.estado = estado;
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
}

