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

import java.time.Instant;
import java.util.UUID;

/**
 * Mecanico del taller. Es un recurso mas del taller (como la bahia), por eso
 * vive en catalog y no en jobs: jobs solo copia su id y nombre en la orden.
 *
 * El correo no es opcional: cuando se le asigna un trabajo, notify le avisa por
 * ese medio en que bahia debe atenderlo.
 */
@Entity
@Table(
        name = "mecanicos",
        uniqueConstraints = @UniqueConstraint(name = "uk_mecanico_taller_rut", columnNames = {"tallerId", "rut"}),
        indexes = @Index(name = "idx_mecanico_taller", columnList = "tallerId")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mecanico {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID tallerId;

    /** RUT normalizado, sin puntos y con guion: "12345678-5". Unico por taller. */
    @Column(nullable = false, length = 12)
    private String rut;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 150)
    private String correo;

    @Column(length = 30)
    private String telefono;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    @Column(nullable = false)
    private Instant fechaActualizacion;

    @Version
    private Long version;

    @PrePersist
    @PreUpdate
    void alGuardar() {
        this.fechaActualizacion = Instant.now();
    }
}
