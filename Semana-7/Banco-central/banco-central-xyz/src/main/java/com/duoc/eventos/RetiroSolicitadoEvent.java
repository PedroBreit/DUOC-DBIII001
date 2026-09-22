package com.duoc.eventos;

import java.math.BigDecimal;
import java.time.Instant;

public record RetiroSolicitadoEvent(
        String solicitudId,
        Long cuentaId,
        BigDecimal monto,
        Instant fechaSolicitud
) {
}
