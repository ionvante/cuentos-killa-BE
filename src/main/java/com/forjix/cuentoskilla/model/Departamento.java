package com.forjix.cuentoskilla.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "tabla_departamento")
public class Departamento {

    @Id
    @Column(name = "id_departamento", length = 2)
    private String id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;
}
