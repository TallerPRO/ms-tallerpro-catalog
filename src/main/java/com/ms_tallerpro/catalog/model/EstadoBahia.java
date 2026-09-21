package com.ms_tallerpro.catalog.model;

/**
 * Ciclo de una bahia: DISPONIBLE -> RESERVADA (orden asignada, vehiculo aun no ingresa)
 * -> OCUPADA (vehiculo en la bahia) -> DISPONIBLE (liberada).
 */
public enum EstadoBahia {
    DISPONIBLE, RESERVADA, OCUPADA
}
