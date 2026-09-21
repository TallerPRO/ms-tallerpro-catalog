package com.ms_tallerpro.catalog.repository;

import com.ms_tallerpro.catalog.model.Repuesto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RepuestoRepository extends JpaRepository<Repuesto, UUID> {

    /** Todo acceso a un repuesto va acotado al taller: nunca se busca solo por id. */
    Optional<Repuesto> findByIdAndTallerId(UUID id, UUID tallerId);

    boolean existsByTallerIdAndCodigo(UUID tallerId, String codigo);

    @Query("""
            select r from Repuesto r
            where r.tallerId = :tallerId
              and (:busqueda is null or lower(r.nombre) like lower(concat('%', :busqueda, '%'))
                                    or lower(r.codigo) like lower(concat('%', :busqueda, '%'))
                                    or lower(r.marca) like lower(concat('%', :busqueda, '%')))
              and (:soloBajoStock = false or r.stock <= r.stockMinimo)
              and (:soloActivos = false or r.activo = true)
            order by r.nombre
            """)
    Page<Repuesto> buscar(@Param("tallerId") UUID tallerId,
                          @Param("busqueda") String busqueda,
                          @Param("soloBajoStock") boolean soloBajoStock,
                          @Param("soloActivos") boolean soloActivos,
                          Pageable pageable);
}
