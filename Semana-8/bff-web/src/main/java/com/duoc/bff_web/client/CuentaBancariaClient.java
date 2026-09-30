package com.duoc.bff_web.client;

import com.duoc.bff_web.dto.central.CuentaBancariaCentralDto;
import com.duoc.bff_web.exception.AccesoDenegadoException;
import com.duoc.bff_web.exception.CuentaNoEncontradaException;
import com.duoc.bff_web.exception.ServicioNoDisponibleException;
import com.duoc.bff_web.exception.TokenInvalidoException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class CuentaBancariaClient {

    private final RestClient bancoCentralRestClient;

    @Retry(name = "bancoCentral")
    @CircuitBreaker(
            name = "bancoCentral",
            fallbackMethod = "obtenerCuentaFallback"
    )
    @RateLimiter(name = "bancoCentral")
    public CuentaBancariaCentralDto obtenerCuenta(Long id, String token) {

        log.info("Consultando cuenta {} en banco-central-xyz", id);

        try {
            return bancoCentralRestClient.get()
                    .uri("/api/cuentas/{id}", id)
                    .header("Authorization", token)
                    .retrieve()
                    .body(CuentaBancariaCentralDto.class);

        } catch (HttpClientErrorException.NotFound ex) {
            throw new CuentaNoEncontradaException(id);

        } catch (HttpClientErrorException.Unauthorized ex) {
            throw new TokenInvalidoException();

        } catch (HttpClientErrorException.Forbidden ex) {
            throw new AccesoDenegadoException();

        } catch (ResourceAccessException ex) {
            throw new ServicioNoDisponibleException();
        }
    }

    private CuentaBancariaCentralDto obtenerCuentaFallback(
            Long id,
            String token,
            Throwable throwable) {

        log.warn(
                "Fallback activado al consultar cuenta {}. Causa: {}",
                id,
                throwable.getMessage()
        );

        throw new ServicioNoDisponibleException();
    }
}