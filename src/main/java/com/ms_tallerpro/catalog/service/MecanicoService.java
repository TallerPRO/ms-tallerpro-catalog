package com.ms_tallerpro.catalog.service;

import com.ms_tallerpro.catalog.dto.MecanicoDtos.MecanicoRequest;
import com.ms_tallerpro.catalog.dto.MecanicoDtos.MecanicoResponse;
import com.ms_tallerpro.catalog.exception.ConflictoException;
import com.ms_tallerpro.catalog.exception.RecursoNoEncontradoException;
import com.ms_tallerpro.catalog.model.Mecanico;
import com.ms_tallerpro.catalog.repository.MecanicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Alta y consulta de los mecanicos de un taller. */
@Service
@Transactional
public class MecanicoService {

    private final MecanicoRepository repository;

    public MecanicoService(MecanicoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<MecanicoResponse> listar(UUID tallerId, boolean soloActivos) {
        List<Mecanico> mecanicos = soloActivos
                ? repository.findByTallerIdAndActivoTrueOrderByNombre(tallerId)
                : repository.findByTallerIdOrderByNombre(tallerId);
        return mecanicos.stream().map(MecanicoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public MecanicoResponse obtener(UUID tallerId, UUID mecanicoId) {
        return MecanicoResponse.from(buscar(tallerId, mecanicoId));
    }

    public MecanicoResponse crear(UUID tallerId, MecanicoRequest request) {
        String rut = normalizarRut(request.rut());
        if (repository.existsByTallerIdAndRut(tallerId, rut)) {
            throw new ConflictoException("Ya existe un mecanico con el RUT " + rut + " en este taller");
        }
        Mecanico mecanico = Mecanico.builder()
                .tallerId(tallerId)
                .rut(rut)
                .nombre(request.nombre().trim())
                .correo(request.correo().trim().toLowerCase())
                .telefono(request.telefono())
                .activo(request.activo() == null || request.activo())
                .build();
        return MecanicoResponse.from(repository.save(mecanico));
    }

    public MecanicoResponse actualizar(UUID tallerId, UUID mecanicoId, MecanicoRequest request) {
        Mecanico mecanico = buscar(tallerId, mecanicoId);
        String rut = normalizarRut(request.rut());
        if (!rut.equals(mecanico.getRut()) && repository.existsByTallerIdAndRut(tallerId, rut)) {
            throw new ConflictoException("Ya existe un mecanico con el RUT " + rut + " en este taller");
        }
        mecanico.setRut(rut);
        mecanico.setNombre(request.nombre().trim());
        mecanico.setCorreo(request.correo().trim().toLowerCase());
        mecanico.setTelefono(request.telefono());
        if (request.activo() != null) {
            mecanico.setActivo(request.activo());
        }
        return MecanicoResponse.from(repository.save(mecanico));
    }

    /** Baja logica: el mecanico deja de ofrecerse, pero las ordenes historicas lo conservan. */
    public void desactivar(UUID tallerId, UUID mecanicoId) {
        Mecanico mecanico = buscar(tallerId, mecanicoId);
        mecanico.setActivo(false);
        repository.save(mecanico);
    }

    private Mecanico buscar(UUID tallerId, UUID mecanicoId) {
        return repository.findByIdAndTallerId(mecanicoId, tallerId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mecanico " + mecanicoId + " no encontrado en el taller " + tallerId));
    }

    /**
     * Deja el RUT como "12345678-5": sin puntos, con guion y K mayuscula, y
     * valida el digito verificador (modulo 11). Se normaliza antes de guardar
     * para que "12.345.678-5" y "123456785" no entren como dos mecanicos.
     */
    static String normalizarRut(String valor) {
        String limpio = valor == null ? "" : valor.toUpperCase().replaceAll("[^0-9K]", "");
        if (limpio.length() < 2) {
            throw new ConflictoException("RUT invalido: " + valor);
        }
        String cuerpo = limpio.substring(0, limpio.length() - 1);
        char dv = limpio.charAt(limpio.length() - 1);
        if (!cuerpo.chars().allMatch(Character::isDigit)) {
            throw new ConflictoException("RUT invalido: " + valor);
        }
        if (dv != calcularDigitoVerificador(cuerpo)) {
            throw new ConflictoException("El digito verificador del RUT " + valor + " no es correcto");
        }
        return cuerpo + "-" + dv;
    }

    private static char calcularDigitoVerificador(String cuerpo) {
        int suma = 0;
        int multiplicador = 2;
        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            suma += (cuerpo.charAt(i) - '0') * multiplicador;
            multiplicador = multiplicador == 7 ? 2 : multiplicador + 1;
        }
        int resto = 11 - (suma % 11);
        if (resto == 11) {
            return '0';
        }
        if (resto == 10) {
            return 'K';
        }
        return (char) ('0' + resto);
    }
}
