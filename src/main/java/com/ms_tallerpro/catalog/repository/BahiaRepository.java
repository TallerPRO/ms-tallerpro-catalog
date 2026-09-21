package com.ms_tallerpro.catalog.repository;

import com.ms_tallerpro.catalog.model.Bahia;
import com.ms_tallerpro.catalog.model.EstadoBahia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BahiaRepository extends JpaRepository<Bahia, UUID> {

    Optional<Bahia> findByIdAndTallerId(UUID id, UUID tallerId);

    boolean existsByTallerIdAndCodigo(UUID tallerId, String codigo);

    List<Bahia> findByTallerIdOrderByCodigo(UUID tallerId);

    List<Bahia> findByTallerIdAndEstadoOrderByCodigo(UUID tallerId, EstadoBahia estado);

    Optional<Bahia> findByOrdenId(UUID ordenId);
}
