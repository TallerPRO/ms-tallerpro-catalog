package com.ms_tallerpro.catalog.exception;

/** Regla de negocio violada: codigo duplicado, stock insuficiente, bahia no disponible... (409). */
public class ConflictoException extends RuntimeException {
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
