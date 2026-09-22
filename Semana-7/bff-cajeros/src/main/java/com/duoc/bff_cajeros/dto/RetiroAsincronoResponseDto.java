package com.duoc.bff_cajeros.dto;

public record RetiroAsincronoResponseDto(
        String solicitudId,
        String estado,
        String mensaje
) {
}
