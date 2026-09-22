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

    /**
     * Busqueda por texto (nombre o codigo) y categoria, ambos opcionales.
     *
     * Los `cast(... as String)` no son decorativos: en PostgreSQL un parametro
     * que solo aparece en `:p is null` no tiene contexto para inferir el tipo y
     * el driver falla con "could not determine data type" o "lower(bytea) does
     * not exist". H2 lo tolera, por eso no se ve en los tests.
     */
    @Query("""
            select s from Servicio s
            where (cast(:busqueda as String) is null
                     or lower(s.nombre) like lower(concat('%', cast(:busqueda as String), '%'))
                     or lower(s.codigo) like lower(concat('%', cast(:busqueda as String), '%')))
              and (cast(:categoria as String) is null or s.categoria = :categoria)
              and (:soloActivos = false or s.activo = true)
            order by s.codigo
            """)
    Page<Servicio> buscar(@Param("busqueda") String busqueda,
                          @Param("categoria") CategoriaServicio categoria,
                          @Param("soloActivos") boolean soloActivos,
                          Pageable pageable);
}
