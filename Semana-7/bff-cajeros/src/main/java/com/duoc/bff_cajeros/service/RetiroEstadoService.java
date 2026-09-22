package com.duoc.bff_cajeros.service;

import com.duoc.bff_cajeros.dto.RetiroEstadoResponseDto;
import com.duoc.eventos.RetiroResultadoEvent;
import com.duoc.eventos.RetiroSolicitadoEvent;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RetiroEstadoService {

    private final Map<String, RetiroEstadoResponseDto> estados = new ConcurrentHashMap<>();

    public void registrarPendiente(RetiroSolicitadoEvent evento) {

        RetiroEstadoResponseDto estado = new RetiroEstadoResponseDto(
                evento.solicitudId(),
                evento.cuentaId(),
                "PENDIENTE",
                evento.monto(),
                null,
                null,
                "Solicitud enviada a Kafka y pendiente de procesamiento",
                null
        );

        estados.put(evento.solicitudId(), estado);
    }

    public void registrarResultado(RetiroResultadoEvent evento) {

        RetiroEstadoResponseDto estado = new RetiroEstadoResponseDto(
                evento.solicitudId(),
                evento.cuentaId(),
                evento.estado(),
                evento.monto(),
                evento.saldoInicial(),
                evento.saldoFinal(),
                evento.motivo(),
                evento.fechaProcesamiento()
        );

        estados.put(evento.solicitudId(), estado);
    }

    public RetiroEstadoResponseDto obtenerEstado(String solicitudId) {

        RetiroEstadoResponseDto estado = estados.get(solicitudId);

        if (estado == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No existe una solicitud de retiro con el identificador indicado"
            );
        }

        return estado;
    }
}
