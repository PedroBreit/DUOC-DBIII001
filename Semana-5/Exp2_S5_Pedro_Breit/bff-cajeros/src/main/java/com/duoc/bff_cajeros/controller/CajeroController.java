package com.duoc.bff_cajeros.controller;

import com.duoc.bff_cajeros.dto.RetiroRequestDto;
import com.duoc.bff_cajeros.dto.RetiroResponseDto;
import com.duoc.bff_cajeros.dto.SaldoResponseDto;
import com.duoc.bff_cajeros.service.CajeroService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bff-cajero/cuentas")
@RequiredArgsConstructor
public class CajeroController {

    private final CajeroService cajeroService;


    /*
     * Consulta de saldo para Cajero.
     *
     * Solo acepta JWT del canal CAJERO.
     */
    @GetMapping("/{id}/saldo")
    @PreAuthorize(
            "hasAnyRole('CLIENTE', 'EMPLEADO') and hasAuthority('CANAL_CAJERO')"
    )
    public SaldoResponseDto obtenerSaldo(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token
    ) {

        return cajeroService.consultarSaldo(
                id,
                token
        );
    }


    /*
     * Realiza un retiro desde el Cajero.
     *
     * Requiere:
     * - rol CLIENTE
     * - canal CAJERO
     *
     * El Banco Central valida además que
     * la cuenta pertenezca al usuario.
     */
    @PostMapping("/{id}/retiros")
    @PreAuthorize(
            "hasRole('CLIENTE') and hasAuthority('CANAL_CAJERO')"
    )
    public RetiroResponseDto retirar(
            @PathVariable Long id,
            @RequestBody RetiroRequestDto request,
            @RequestHeader("Authorization") String token
    ) {

        return cajeroService.retirar(
                id,
                request,
                token
        );
    }
}
