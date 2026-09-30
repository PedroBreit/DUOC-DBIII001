package com.duoc.bff_web.client;

import com.duoc.bff_web.dto.central.TransaccionCentralDto;
import com.duoc.bff_web.exception.AccesoDenegadoException;
import com.duoc.bff_web.exception.ServicioNoDisponibleException;
import com.duoc.bff_web.exception.TokenInvalidoException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransaccionClient {

    private final RestClient bancoCentralRestClient;

    @Retry(name = "bancoCentral")
    @CircuitBreaker(
            name = "bancoCentral",
            fallbackMethod = "listarTransaccionesFallback"
    )
    @RateLimiter(name = "bancoCentral")
    public List<TransaccionCentralDto> listarTransacciones(String token) {

        log.info("Consultando transacciones recientes en banco-central-xyz");

        try {
            return bancoCentralRestClient.get()
                    .uri("/api/transacciones/recientes")
                    .header("Authorization", token)
                    .retrieve()
                    .body(
                            new ParameterizedTypeReference<
                                    List<TransaccionCentralDto>
                                    >() {
                            }
                    );

        } catch (HttpClientErrorException.Unauthorized ex) {
            throw new TokenInvalidoException();

        } catch (HttpClientErrorException.Forbidden ex) {
            throw new AccesoDenegadoException();

        } catch (ResourceAccessException ex) {
            throw new ServicioNoDisponibleException();
        }
    }

    private List<TransaccionCentralDto> listarTransaccionesFallback(
            String token,
            Throwable throwable) {

        log.warn(
                "Fallback activado al consultar transacciones. Causa: {}",
                throwable.getMessage()
        );

        throw new ServicioNoDisponibleException();
    }
}