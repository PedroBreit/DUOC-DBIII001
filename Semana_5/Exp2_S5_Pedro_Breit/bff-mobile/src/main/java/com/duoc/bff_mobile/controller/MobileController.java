package com.duoc.bff_mobile.controller;

import com.duoc.bff_mobile.dto.SaldoResponseDto;
import com.duoc.bff_mobile.service.MobileService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bff-mobile/cuentas")
@RequiredArgsConstructor
public class MobileController {

    private final MobileService mobileService;


    /*
     * Endpoint optimizado para dispositivos móviles.
     *
     * Solo permite:
     *
     * - CLIENTE o EMPLEADO
     * - JWT generado específicamente para canal MOBILE
     */
    @GetMapping("/{id}/saldo")
    @PreAuthorize(
            "hasAnyRole('CLIENTE', 'EMPLEADO') and hasAuthority('CANAL_MOBILE')"
    )
    public SaldoResponseDto obtenerSaldo(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token
    ) {

        /*
         * Se reenvía el JWT al Backend Central para que
         * también aplique sus validaciones de seguridad.
         */
        return mobileService.consultarSaldo(id, token);
    }
}
