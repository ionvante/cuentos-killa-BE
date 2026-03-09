package com.forjix.cuentoskilla.model.DTOs;

public class UserProfileDTO {
    private String nombre;
    private String apellido;
    private String telefono;
    private String documentoTipo;
    private String documentoNumero;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getDocumentoTipo() { return documentoTipo; }
    public void setDocumentoTipo(String documentoTipo) { this.documentoTipo = documentoTipo; }

    public String getDocumentoNumero() { return documentoNumero; }
    public void setDocumentoNumero(String documentoNumero) { this.documentoNumero = documentoNumero; }
}
