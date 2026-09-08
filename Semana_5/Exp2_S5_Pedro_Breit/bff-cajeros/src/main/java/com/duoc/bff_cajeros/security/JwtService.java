package com.duoc.bff_cajeros.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;


    /*
     * Convierte el JWT_SECRET en una clave válida
     * para verificar la firma del token.
     */
    private SecretKey getSigningKey() {

        return Keys.hmacShaKeyFor(
                secretKey.getBytes(StandardCharsets.UTF_8)
        );
    }


    /*
     * Valida la firma y la expiración del JWT.
     *
     * Si el token es correcto devuelve sus claims.
     */
    public Claims validarToken(String token) {

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


    /*
     * Obtiene el username almacenado en el subject.
     */
    public String obtenerUsername(String token) {

        return validarToken(token)
                .getSubject();
    }


    /*
     * Obtiene el rol incluido por Banco Central.
     *
     * Ejemplo:
     * CLIENTE
     * EMPLEADO
     */
    public String obtenerRol(String token) {

        return validarToken(token)
                .get("rol", String.class);
    }


    /*
     * Obtiene el canal incluido en el JWT.
     *
     * Para este BFF esperamos:
     * CAJERO
     */
    public String obtenerCanal(String token) {

        return validarToken(token)
                .get("canal", String.class);
    }
}
