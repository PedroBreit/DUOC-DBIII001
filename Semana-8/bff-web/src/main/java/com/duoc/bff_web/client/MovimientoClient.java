package com.duoc.bff_web.client;

import com.duoc.bff_web.dto.central.MovimientoCentralDto;
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
public class MovimientoClient {

    private final RestClient bancoCentralRestClient;

    @Retry(name = "bancoCentral")
    @CircuitBreaker(
            name = "bancoCentral",
            fallbackMethod = "listarMovimientosFallback"
    )
    @RateLimiter(name = "bancoCentral")
    public List<MovimientoCentralDto> listarMovimientos(String token) {

        log.info("Consultando movimientos recientes en banco-central-xyz");

        try {
            return bancoCentralRestClient.get()
                    .uri("/api/movimientos/recientes")
                    .header("Authorization", token)
                    .retrieve()
                    .body(
                            new ParameterizedTypeReference<
                                    List<MovimientoCentralDto>
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

    private List<MovimientoCentralDto> listarMovimientosFallback(
            String token,
            Throwable throwable) {

        log.warn(
                "Fallback activado al consultar movimientos. Causa: {}",
                throwable.getMessage()
        );

        throw new ServicioNoDisponibleException();
    }
}