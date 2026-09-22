package com.duoc.bff_cajeros.dto.central;

import java.math.BigDecimal;

public record RetiroCentralRequestDto(
        BigDecimal monto
) {
}