package com.duoc.bff_web.controller;

import com.duoc.bff_web.dto.CuentaBancariaResponseDto;
import com.duoc.bff_web.service.WebService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bff-web/cuentas")
@RequiredArgsConstructor
public class WebController {

    private final WebService webService;

    /*
     * El endpoint Web requiere:
     *
     * 1. Usuario CLIENTE o EMPLEADO.
     * 2. Token emitido específicamente para WEB.
     */
    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('CLIENTE', 'EMPLEADO') " +
            "and hasAuthority('CANAL_WEB')"
    )
    public CuentaBancariaResponseDto obtenerCuenta(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token
    ) {

        /*
         * El mismo JWT se reenvía al Backend Central,
         * que realiza además la validación de acceso
         * a la cuenta bancaria solicitada.
         */
        return webService.consultarCuenta(
                id,
                token
        );
    }
}
