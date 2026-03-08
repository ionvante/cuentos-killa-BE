package com.forjix.cuentoskilla.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "boletas")
public class Boleta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(name = "numero_comprobante", nullable = false, unique = true, length = 30)
    private String numeroComprobante;

    @Column(name = "serie", nullable = false, length = 10)
    private String serie;

    @Column(name = "correlativo", nullable = false)
    private Integer correlativo;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_generacion", nullable = false, length = 20)
    private BoletaGeneracionEstado estadoGeneracion = BoletaGeneracionEstado.PENDIENTE;

    @Column(name = "intentos", nullable = false)
    private Integer intentos = 0;

    @Column(name = "ultimo_error", length = 500)
    private String ultimoError;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (fechaEmision == null) {
            fechaEmision = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (estadoGeneracion == null) {
            estadoGeneracion = BoletaGeneracionEstado.PENDIENTE;
        }
        if (intentos == null) {
            intentos = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        if (intentos == null) {
            intentos = 0;
        }
        if (estadoGeneracion == null) {
            estadoGeneracion = BoletaGeneracionEstado.PENDIENTE;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public String getNumeroComprobante() {
        return numeroComprobante;
    }

    public void setNumeroComprobante(String numeroComprobante) {
        this.numeroComprobante = numeroComprobante;
    }

    public String getSerie() {
        return serie;
    }

    public void setSerie(String serie) {
        this.serie = serie;
    }

    public Integer getCorrelativo() {
        return correlativo;
    }

    public void setCorrelativo(Integer correlativo) {
        this.correlativo = correlativo;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public LocalDateTime getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDateTime fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public BoletaGeneracionEstado getEstadoGeneracion() {
        return estadoGeneracion;
    }

    public void setEstadoGeneracion(BoletaGeneracionEstado estadoGeneracion) {
        this.estadoGeneracion = estadoGeneracion;
    }

    public Integer getIntentos() {
        return intentos;
    }

    public void setIntentos(Integer intentos) {
        this.intentos = intentos;
    }

    public String getUltimoError() {
        return ultimoError;
    }

    public void setUltimoError(String ultimoError) {
        this.ultimoError = ultimoError;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
