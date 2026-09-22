package com.duoc.banco_central_xyz.controller;

import com.duoc.banco_central_xyz.dto.CuentaBancariaDto;
import com.duoc.banco_central_xyz.dto.CuentaBancariaSaldoDto;
import com.duoc.banco_central_xyz.dto.RetiroCuentaBancariaRequestDto;
import com.duoc.banco_central_xyz.dto.RetiroCuentaBancariaResponseDto;
import com.duoc.banco_central_xyz.service.CuentaBancariaService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cuentas")
public class CuentaBancariaController {

    private final CuentaBancariaService cuentaBancariaService;


    public CuentaBancariaController(
            CuentaBancariaService cuentaBancariaService
    ) {

        this.cuentaBancariaService =
                cuentaBancariaService;
    }


    /*
     * Solo un EMPLEADO puede listar todas
     * las cuentas bancarias.
     */
    @GetMapping
    @PreAuthorize("hasRole('EMPLEADO')")
    public List<CuentaBancariaDto> listarTodas() {

        return cuentaBancariaService
                .listarTodas();
    }


    /*
     * Obtiene la información completa
     * de una cuenta.
     */
    @GetMapping("/{id}")
    public CuentaBancariaDto obtenerPorId(
            @PathVariable Long id,
            Authentication authentication
    ) {

        return cuentaBancariaService.obtenerPorId(
                id,
                authentication.getName()
        );
    }


    /*
     * Consulta únicamente el saldo.
     */
    @GetMapping("/{id}/saldo")
    public CuentaBancariaSaldoDto obtenerSaldo(
            @PathVariable Long id,
            Authentication authentication
    ) {

        return cuentaBancariaService.obtenerSaldo(
                id,
                authentication.getName()
        );
    }


    /*
     * Realiza un retiro desde una cuenta.
     *
     * Solo un usuario con rol CLIENTE puede
     * realizar esta operación.
     *
     * Además, el Service valida que la cuenta
     * pertenezca realmente al usuario.
     */
    @PostMapping("/{id}/retiros")
    @PreAuthorize("hasRole('CLIENTE')")
    public RetiroCuentaBancariaResponseDto retirar(
            @PathVariable Long id,
            @RequestBody RetiroCuentaBancariaRequestDto request,
            Authentication authentication
    ) {

        return cuentaBancariaService.retirar(
                id,
                request,
                authentication.getName()
        );
    }
}
