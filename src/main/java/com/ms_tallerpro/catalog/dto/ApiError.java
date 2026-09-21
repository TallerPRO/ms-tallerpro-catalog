package com.ms_tallerpro.catalog.dto;

import java.time.Instant;

/** Mismo formato de error que ms-tallerpro-jobs, para que el frontend lo trate igual. */
public record ApiError(Instant timestamp, int status, String error, String message, String path) {}
