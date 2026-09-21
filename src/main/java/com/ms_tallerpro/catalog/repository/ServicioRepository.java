package com.ms_tallerpro.catalog.repository;

import com.ms_tallerpro.catalog.model.CategoriaServicio;
import com.ms_tallerpro.catalog.model.Servicio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ServicioRepository extends JpaRepository<Servicio, UUID> {

    Optional<Servicio> findByCodigo(String codigo);

    /** Busqueda por texto (nombre o codigo) y categoria, ambos opcionales. */
    @Query("""
            select s from Servicio s
            where (:busqueda is null or lower(s.nombre) like lower(concat('%', :busqueda, '%'))
                                    or lower(s.codigo) like lower(concat('%', :busqueda, '%')))
              and (:categoria is null or s.categoria = :categoria)
              and (:soloActivos = false or s.activo = true)
            order by s.codigo
            """)
    Page<Servicio> buscar(@Param("busqueda") String busqueda,
                          @Param("categoria") CategoriaServicio categoria,
                          @Param("soloActivos") boolean soloActivos,
                          Pageable pageable);
}
