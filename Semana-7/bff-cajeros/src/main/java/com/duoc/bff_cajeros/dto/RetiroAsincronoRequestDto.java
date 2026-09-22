package com.duoc.bff_cajeros.dto;

import java.math.BigDecimal;

public record RetiroAsincronoRequestDto(
        BigDecimal monto
) {
}
