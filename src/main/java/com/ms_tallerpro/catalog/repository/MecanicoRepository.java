package com.ms_tallerpro.catalog.repository;

import com.ms_tallerpro.catalog.model.Mecanico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MecanicoRepository extends JpaRepository<Mecanico, UUID> {

    /** Todo acceso a un mecanico va acotado al taller: nunca se busca solo por id. */
    Optional<Mecanico> findByIdAndTallerId(UUID id, UUID tallerId);

    boolean existsByTallerIdAndRut(UUID tallerId, String rut);

    List<Mecanico> findByTallerIdOrderByNombre(UUID tallerId);

    List<Mecanico> findByTallerIdAndActivoTrueOrderByNombre(UUID tallerId);
}
