package com.duoc.bff_cajeros.kafka;

import com.duoc.eventos.RetiroSolicitadoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetiroKafkaProducer {

    private static final String TOPIC_RETIROS_SOLICITADOS = "retiros-solicitados";

    private final KafkaTemplate<String, RetiroSolicitadoEvent> kafkaTemplate;

    public void publicarSolicitud(RetiroSolicitadoEvent evento) {

        String key = evento.cuentaId().toString();

        log.info(
                "Publicando retiro solicitudId={} cuentaId={} monto={} topic={}",
                evento.solicitudId(),
                evento.cuentaId(),
                evento.monto(),
                TOPIC_RETIROS_SOLICITADOS
        );

        kafkaTemplate
                .send(TOPIC_RETIROS_SOLICITADOS, key, evento)
                .whenComplete((resultado, error) -> {

                    if (error != null) {

                        log.error(
                                "Error publicando solicitudId={} en Kafka: {}",
                                evento.solicitudId(),
                                error.getMessage()
                        );

                        return;
                    }

                    log.info(
                            "Solicitud publicada correctamente solicitudId={} topic={} partition={} offset={}",
                            evento.solicitudId(),
                            resultado.getRecordMetadata().topic(),
                            resultado.getRecordMetadata().partition(),
                            resultado.getRecordMetadata().offset()
                    );
                });
    }
}
