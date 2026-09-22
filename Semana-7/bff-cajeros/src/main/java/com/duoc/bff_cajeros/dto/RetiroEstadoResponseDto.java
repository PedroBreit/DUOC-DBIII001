package com.duoc.bff_cajeros.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record RetiroEstadoResponseDto(
        String solicitudId,
        Long cuentaId,
        String estado,
        BigDecimal monto,
        BigDecimal saldoInicial,
        BigDecimal saldoFinal,
        String motivo,
        Instant fechaProcesamiento
) {
}
