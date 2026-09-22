package com.duoc.bff_mobile.security;

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
     * Convierte el texto definido en JWT_SECRET
     * en una clave válida para verificar la firma del JWT.
     */
    private SecretKey getSigningKey() {

        return Keys.hmacShaKeyFor(
                secretKey.getBytes(StandardCharsets.UTF_8)
        );
    }


    /*
     * Valida la firma y expiración del token.
     *
     * Si el JWT es válido retorna sus claims.
     * Si es inválido o está expirado JJWT lanza una excepción.
     */
    public Claims validarToken(String token) {

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


    /*
     * Obtiene el nombre del usuario almacenado
     * en el campo "subject" del JWT.
     */
    public String obtenerUsername(String token) {

        return validarToken(token).getSubject();
    }


    /*
     * Obtiene el rol incluido por el Banco Central.
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
     * Obtiene el canal para el cual fue generado el JWT.
     *
     * Ejemplo:
     * WEB
     * MOBILE
     * CAJERO
     */
    public String obtenerCanal(String token) {

        return validarToken(token)
                .get("canal", String.class);
    }
}
