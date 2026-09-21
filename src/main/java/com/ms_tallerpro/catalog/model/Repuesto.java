package com.ms_tallerpro.catalog.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Repuesto con stock POR TALLER: el mismo numero de parte puede existir en varios
 * talleres, cada uno con su propio inventario (RF-08: el stock disminuye al
 * diagnosticar la orden, y ms-tallerpro-jobs siempre indica el taller).
 */
@Entity
@Table(
        name = "repuestos",
        uniqueConstraints = @UniqueConstraint(name = "uk_repuesto_taller_codigo", columnNames = {"tallerId", "codigo"}),
        indexes = @Index(name = "idx_repuesto_taller", columnList = "tallerId")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Repuesto {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID tallerId;

    /** Numero de parte del fabricante, ej. "BR-4521-T". */
    @Column(nullable = false, length = 40)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 80)
    private String marca;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false)
    private Integer stock;

    /** Bajo este umbral el repuesto se reporta como "stock bajo". */
    @Column(nullable = false)
    private Integer stockMinimo;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    @Column(nullable = false, updatable = false)
    private Instant fechaCreacion;

    @Column(nullable = false)
    private Instant fechaActualizacion;

    /** Bloqueo optimista: dos diagnosticos simultaneos no pueden dejar el stock negativo. */
    @Version
    private Long version;

    public boolean bajoStock() {
        return stock <= stockMinimo;
    }

    @PrePersist
    void alCrear() {
        Instant ahora = Instant.now();
        fechaCreacion = ahora;
        fechaActualizacion = ahora;
    }

    @PreUpdate
    void alActualizar() {
        fechaActualizacion = Instant.now();
    }
}
