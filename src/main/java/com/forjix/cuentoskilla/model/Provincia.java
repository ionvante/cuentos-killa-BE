package com.forjix.cuentoskilla.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Data
@Entity
@Table(name = "tabla_provincia")
public class Provincia {

    @Id
    @Column(name = "id_provincia", length = 4)
    private String id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_departamento", nullable = false)
    @ToString.Exclude
    @JsonIgnore
    private Departamento departamento;
}
