package com.ms_tallerpro.catalog.repository;

import com.ms_tallerpro.catalog.model.MovimientoStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, UUID> {

    Optional<MovimientoStock> findByEventId(String eventId);

    List<MovimientoStock> findByRepuestoIdOrderByFechaDesc(UUID repuestoId);
}
