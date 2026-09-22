package com.duoc.bff_cajeros.service;

import com.duoc.bff_cajeros.client.CuentaBancariaClient;
import com.duoc.bff_cajeros.dto.RetiroAsincronoRequestDto;
import com.duoc.bff_cajeros.dto.RetiroAsincronoResponseDto;
import com.duoc.bff_cajeros.dto.RetiroEstadoResponseDto;
import com.duoc.bff_cajeros.dto.SaldoResponseDto;
import com.duoc.bff_cajeros.dto.central.SaldoCentralDto;
import com.duoc.bff_cajeros.kafka.RetiroKafkaProducer;
import com.duoc.eventos.RetiroSolicitadoEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CajeroService {

    private final CuentaBancariaClient cuentaBancariaClient;
    private final RetiroKafkaProducer retiroKafkaProducer;
    private final RetiroEstadoService retiroEstadoService;

    public SaldoResponseDto consultarSaldo(Long id, String token) {

        SaldoCentralDto saldo = cuentaBancariaClient.obtenerSaldo(id, token);

        return new SaldoResponseDto(
                saldo.saldo()
        );
    }

    public RetiroAsincronoResponseDto solicitarRetiro(
            Long cuentaId,
            RetiroAsincronoRequestDto request,
            String token
    ) {

        validarMonto(request);

        /*
         * Esta llamada valida que:
         *
         * 1. La cuenta exista.
         * 2. El token sea valido.
         * 3. El usuario tenga acceso a la cuenta.
         *
         * Ademas permanece protegida mediante Resilience4j.
         */
        cuentaBancariaClient.obtenerSaldo(cuentaId, token);

        String solicitudId = UUID.randomUUID().toString();

        RetiroSolicitadoEvent evento = new RetiroSolicitadoEvent(
                solicitudId,
                cuentaId,
                request.monto(),
                Instant.now()
        );

        retiroEstadoService.registrarPendiente(evento);

        retiroKafkaProducer.publicarSolicitud(evento);

        return new RetiroAsincronoResponseDto(
                solicitudId,
                "PENDIENTE",
                "La solicitud de retiro fue enviada para procesamiento asincrono"
        );
    }

    public RetiroEstadoResponseDto consultarEstadoRetiro(String solicitudId) {
        return retiroEstadoService.obtenerEstado(solicitudId);
    }

    private void validarMonto(RetiroAsincronoRequestDto request) {

        if (request == null ||
                request.monto() == null ||
                request.monto().compareTo(BigDecimal.ZERO) <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El monto del retiro debe ser mayor que cero"
            );
        }
    }
}
