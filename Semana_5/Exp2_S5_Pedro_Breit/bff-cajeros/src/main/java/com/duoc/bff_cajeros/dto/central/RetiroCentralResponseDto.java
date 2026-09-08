package com.duoc.bff_cajeros.dto.central;

import java.math.BigDecimal;

public record RetiroCentralResponseDto(
        BigDecimal saldoInicial,
        BigDecimal montoRetirado,
        BigDecimal saldoFinal
) {
}
