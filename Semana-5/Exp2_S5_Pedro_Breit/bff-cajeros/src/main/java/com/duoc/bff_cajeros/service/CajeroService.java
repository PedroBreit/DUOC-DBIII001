package com.duoc.bff_cajeros.service;

import com.duoc.bff_cajeros.client.CuentaBancariaClient;

import com.duoc.bff_cajeros.dto.RetiroRequestDto;
import com.duoc.bff_cajeros.dto.RetiroResponseDto;
import com.duoc.bff_cajeros.dto.SaldoResponseDto;

import com.duoc.bff_cajeros.dto.central.RetiroCentralRequestDto;
import com.duoc.bff_cajeros.dto.central.RetiroCentralResponseDto;
import com.duoc.bff_cajeros.dto.central.SaldoCentralDto;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CajeroService {

    private final CuentaBancariaClient cuentaBancariaClient;


    /*
     * Consulta el saldo mediante el Backend Central
     * y expone solamente el dato necesario para el cajero.
     */
    public SaldoResponseDto consultarSaldo(
            Long id,
            String token
    ) {

        SaldoCentralDto saldo =
                cuentaBancariaClient.obtenerSaldo(
                        id,
                        token
                );


        return new SaldoResponseDto(
                saldo.saldo()
        );
    }


    /*
     * Procesa una solicitud de retiro enviada
     * desde el canal Cajero.
     */
    public RetiroResponseDto retirar(
            Long id,
            RetiroRequestDto request,
            String token
    ) {

        /*
         * Transformamos el DTO público del Cajero
         * al DTO utilizado para comunicarnos con
         * el Backend Central.
         */
        RetiroCentralRequestDto requestCentral =
                new RetiroCentralRequestDto(
                        request.monto()
                );


        /*
         * Enviamos el retiro al Backend Central.
         *
         * El token se reenvía para que el Backend
         * Central pueda comprobar usuario y propiedad
         * de la cuenta.
         */
        RetiroCentralResponseDto responseCentral =
                cuentaBancariaClient.retirar(
                        id,
                        requestCentral,
                        token
                );


        /*
         * Adaptamos la respuesta.
         *
         * Backend Central entrega:
         *
         * saldoInicial
         * montoRetirado
         * saldoFinal
         *
         * Cajero solamente entrega:
         *
         * montoRetirado
         * saldoDisponible
         */
        return new RetiroResponseDto(
                responseCentral.montoRetirado(),
                responseCentral.saldoFinal()
        );
    }
}
