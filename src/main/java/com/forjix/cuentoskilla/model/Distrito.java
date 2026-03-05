package com.forjix.cuentoskilla.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Data
@Entity
@Table(name = "tabla_distrito")
public class Distrito {

    @Id
    @Column(name = "id_distrito", length = 6)
    private String id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_provincia", nullable = false)
    @ToString.Exclude
    @JsonIgnore
    private Provincia provincia;
}
