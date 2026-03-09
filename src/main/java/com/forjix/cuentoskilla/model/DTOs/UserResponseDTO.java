package com.forjix.cuentoskilla.model.DTOs;

import com.forjix.cuentoskilla.model.Rol;
import com.forjix.cuentoskilla.model.User;

public class UserResponseDTO {
    private Long id;
    private String email;
    private String nombre;
    private String apellido;
    private String telefono;
    private String documentoTipo;
    private String documentoNumero;
    private Rol role;

    public static UserResponseDTO from(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setNombre(user.getNombre());
        dto.setApellido(user.getApellido());
        dto.setTelefono(user.getTelefono());
        dto.setDocumentoTipo(user.getDocumentoTipo());
        dto.setDocumentoNumero(user.getDocumentoNumero());
        dto.setRole(user.getRole());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDocumentoTipo() {
        return documentoTipo;
    }

    public void setDocumentoTipo(String documentoTipo) {
        this.documentoTipo = documentoTipo;
    }

    public String getDocumentoNumero() {
        return documentoNumero;
    }

    public void setDocumentoNumero(String documentoNumero) {
        this.documentoNumero = documentoNumero;
    }

    public Rol getRole() {
        return role;
    }

    public void setRole(Rol role) {
        this.role = role;
    }
}
