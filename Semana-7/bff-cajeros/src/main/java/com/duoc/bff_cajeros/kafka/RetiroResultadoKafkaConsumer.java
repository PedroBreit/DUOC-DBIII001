package com.duoc.bff_cajeros.kafka;

import com.duoc.bff_cajeros.service.RetiroEstadoService;
import com.duoc.eventos.RetiroResultadoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetiroResultadoKafkaConsumer {

    private final RetiroEstadoService retiroEstadoService;

    @KafkaListener(
            topics = {
                    "retiros-aprobados",
                    "retiros-rechazados"
            },
            groupId = "bff-cajeros-resultados",
            concurrency = "3"
    )
    public void consumirResultado(
            ConsumerRecord<String, RetiroResultadoEvent> record
    ) {

        RetiroResultadoEvent evento = record.value();

        log.info(
                "Resultado recibido solicitudId={} estado={} topic={} partition={} offset={} thread={}",
                evento.solicitudId(),
                evento.estado(),
                record.topic(),
                record.partition(),
                record.offset(),
                Thread.currentThread().getName()
        );

        retiroEstadoService.registrarResultado(evento);
    }
}
