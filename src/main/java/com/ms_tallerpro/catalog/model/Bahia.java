package com.ms_tallerpro.catalog.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Bahia: puesto fisico de trabajo de un taller donde se estaciona el vehiculo
 * mientras se ejecuta la orden. Su codigo ("A-01") es unico dentro del taller.
 */
@Entity
@Table(
        name = "bahias",
        uniqueConstraints = @UniqueConstraint(name = "uk_bahia_taller_codigo", columnNames = {"tallerId", "codigo"}),
        indexes = @Index(name = "idx_bahia_taller", columnList = "tallerId")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bahia {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID tallerId;

    @Column(nullable = false, length = 10)
    private String codigo;

    /** "Mecanica General", "Frenos y Suspension", ... */
    @Column(nullable = false, length = 60)
    private String sector;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private EstadoBahia estado = EstadoBahia.DISPONIBLE;

    /** Orden de servicio asignada (RESERVADA / OCUPADA). Null si esta DISPONIBLE. */
    private UUID ordenId;

    /** Inicio y termino programado de los trabajos, informados al reservar. */
    private Instant inicioProgramado;
    private Instant terminoProgramado;

    /** Desde cuando esta libre; solo tiene sentido en DISPONIBLE. */
    private Instant libreDesde;

    @Column(nullable = false)
    @Builder.Default
    private boolean activa = true;

    @Column(nullable = false, updatable = false)
    private Instant fechaCreacion;

    @Column(nullable = false)
    private Instant fechaActualizacion;

    /** Dos jefes de taller no pueden reservar la misma bahia al mismo tiempo. */
    @Version
    private Long version;

    public boolean disponible() {
        return activa && estado == EstadoBahia.DISPONIBLE;
    }

    @PrePersist
    void alCrear() {
        Instant ahora = Instant.now();
        fechaCreacion = ahora;
        fechaActualizacion = ahora;
        if (libreDesde == null && estado == EstadoBahia.DISPONIBLE) {
            libreDesde = ahora;
        }
    }

    @PreUpdate
    void alActualizar() {
        fechaActualizacion = Instant.now();
    }
}
