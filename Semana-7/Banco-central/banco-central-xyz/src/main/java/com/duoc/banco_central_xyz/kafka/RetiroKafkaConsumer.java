package com.duoc.banco_central_xyz.kafka;

import com.duoc.banco_central_xyz.dto.RetiroCuentaBancariaRequestDto;
import com.duoc.banco_central_xyz.dto.RetiroCuentaBancariaResponseDto;
import com.duoc.banco_central_xyz.exception.CuentaNoEncontradaException;
import com.duoc.banco_central_xyz.exception.FondosInsuficientesException;
import com.duoc.banco_central_xyz.exception.OperacionNoPermitidaException;
import com.duoc.banco_central_xyz.service.CuentaBancariaService;
import com.duoc.eventos.RetiroResultadoEvent;
import com.duoc.eventos.RetiroSolicitadoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetiroKafkaConsumer {

    private final CuentaBancariaService cuentaBancariaService;
    private final RetiroResultadoKafkaProducer retiroResultadoKafkaProducer;

    @KafkaListener(
            topics = "retiros-solicitados",
            groupId = "banco-central-retiros",
            concurrency = "3"
    )
    public void procesarRetiro(
            ConsumerRecord<String, RetiroSolicitadoEvent> record
    ) {

        RetiroSolicitadoEvent evento = record.value();

        log.info(
                "Procesando retiro solicitudId={} cuentaId={} monto={} partition={} offset={} thread={}",
                evento.solicitudId(),
                evento.cuentaId(),
                evento.monto(),
                record.partition(),
                record.offset(),
                Thread.currentThread().getName()
        );

        try {

            RetiroCuentaBancariaResponseDto respuesta =
                    cuentaBancariaService.retirar(
                            evento.cuentaId(),
                            new RetiroCuentaBancariaRequestDto(evento.monto())
                    );

            RetiroResultadoEvent resultado = new RetiroResultadoEvent(
                    evento.solicitudId(),
                    evento.cuentaId(),
                    "APROBADO",
                    evento.monto(),
                    respuesta.saldoInicial(),
                    respuesta.saldoFinal(),
                    "Retiro procesado correctamente",
                    Instant.now()
            );

            retiroResultadoKafkaProducer.publicarResultado(resultado);

        } catch (
                CuentaNoEncontradaException |
                FondosInsuficientesException |
                OperacionNoPermitidaException ex
        ) {

            log.warn(
                    "Retiro rechazado solicitudId={} motivo={}",
                    evento.solicitudId(),
                    ex.getMessage()
            );

            RetiroResultadoEvent resultado = new RetiroResultadoEvent(
                    evento.solicitudId(),
                    evento.cuentaId(),
                    "RECHAZADO",
                    evento.monto(),
                    null,
                    null,
                    ex.getMessage(),
                    Instant.now()
            );

            retiroResultadoKafkaProducer.publicarResultado(resultado);

        } catch (Exception ex) {

            /*
             * Los errores inesperados no se convierten en un rechazo
             * de negocio. Se propagan para que Kafka pueda volver a
             * entregar el mensaje.
             */

            log.error(
                    "Error tecnico procesando solicitudId={}",
                    evento.solicitudId(),
                    ex
            );

            throw ex;
        }
    }
}
