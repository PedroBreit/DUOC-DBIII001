package com.duoc.bff_cajeros.dto;

import java.math.BigDecimal;

public record RetiroResponseDto(
        BigDecimal montoRetirado,
        BigDecimal saldoDisponible
) {
}
