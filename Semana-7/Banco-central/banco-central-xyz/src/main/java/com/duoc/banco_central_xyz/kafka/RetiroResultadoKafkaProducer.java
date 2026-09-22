package com.duoc.banco_central_xyz.kafka;

import com.duoc.eventos.RetiroResultadoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetiroResultadoKafkaProducer {

    private static final String TOPIC_APROBADOS = "retiros-aprobados";
    private static final String TOPIC_RECHAZADOS = "retiros-rechazados";

    private final KafkaTemplate<String, RetiroResultadoEvent> kafkaTemplate;

    public void publicarResultado(RetiroResultadoEvent evento) {

        String topic = "APROBADO".equals(evento.estado())
                ? TOPIC_APROBADOS
                : TOPIC_RECHAZADOS;

        String key = evento.cuentaId().toString();

        kafkaTemplate
                .send(topic, key, evento)
                .whenComplete((resultado, error) -> {

                    if (error != null) {

                        log.error(
                                "Error publicando resultado solicitudId={}: {}",
                                evento.solicitudId(),
                                error.getMessage()
                        );

                        return;
                    }

                    log.info(
                            "Resultado publicado solicitudId={} estado={} topic={} partition={} offset={}",
                            evento.solicitudId(),
                            evento.estado(),
                            resultado.getRecordMetadata().topic(),
                            resultado.getRecordMetadata().partition(),
                            resultado.getRecordMetadata().offset()
                    );
                });
    }
}
