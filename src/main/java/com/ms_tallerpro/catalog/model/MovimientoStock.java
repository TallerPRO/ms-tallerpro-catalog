package com.ms_tallerpro.catalog.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Historial de movimientos de stock (kardex). Ademas de trazabilidad, garantiza
 * idempotencia: ms-tallerpro-jobs envia un eventId por cada decremento y si
 * reintenta (timeout, redelivery) el mismo evento no descuenta dos veces.
 */
@Entity
@Table(
        name = "movimientos_stock",
        indexes = @Index(name = "idx_mov_repuesto", columnList = "repuestoId")
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoStock {

    @Id
    @GeneratedValue
    private UUID id;

    /** Id del evento origen (cabecera X-Event-Id o campo eventId). Unico. */
    @Column(nullable = false, unique = true, length = 100)
    private String eventId;

    @Column(nullable = false)
    private UUID repuestoId;

    @Column(nullable = false)
    private UUID tallerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TipoMovimientoStock tipo;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false)
    private Integer stockResultante;

    @Column(length = 200)
    private String motivo;

    @Column(nullable = false, updatable = false)
    private Instant fecha;

    @PrePersist
    void alCrear() {
        fecha = Instant.now();
    }
}
