package com.duoc.bff_cajeros.controller;

import com.duoc.bff_cajeros.dto.RetiroAsincronoRequestDto;
import com.duoc.bff_cajeros.dto.RetiroAsincronoResponseDto;
import com.duoc.bff_cajeros.dto.RetiroEstadoResponseDto;
import com.duoc.bff_cajeros.dto.SaldoResponseDto;
import com.duoc.bff_cajeros.service.CajeroService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bff-cajero/cuentas")
@RequiredArgsConstructor
public class CajeroController {

    private final CajeroService cajeroService;

    @GetMapping("/{id}/saldo")
    public SaldoResponseDto obtenerSaldo(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token
    ) {

        return cajeroService.consultarSaldo(id, token);
    }

    @PostMapping("/{id}/retiros")
    public ResponseEntity<RetiroAsincronoResponseDto> solicitarRetiro(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token,
            @RequestBody RetiroAsincronoRequestDto request
    ) {

        RetiroAsincronoResponseDto respuesta =
                cajeroService.solicitarRetiro(id, request, token);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(respuesta);
    }

    @GetMapping("/retiros/{solicitudId}")
    public RetiroEstadoResponseDto consultarEstadoRetiro(
            @PathVariable String solicitudId
    ) {

        return cajeroService.consultarEstadoRetiro(solicitudId);
    }
}
