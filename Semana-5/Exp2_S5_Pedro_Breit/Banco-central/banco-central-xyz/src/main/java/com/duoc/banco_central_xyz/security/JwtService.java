package com.duoc.banco_central_xyz.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    private static final long EXPIRACION_MS = 3600_000; // 1 hora

    /*
     * Convierte el secreto configurado en una clave HMAC
     * que permite firmar y validar los JWT.
     */
    private SecretKey getSigningKey() {

        return Keys.hmacShaKeyFor(
                secretKey.getBytes(StandardCharsets.UTF_8)
        );
    }

    /*
     * Genera el JWT del usuario.
     *
     * Además del usuario y rol, ahora guarda el canal
     * desde el cual se autenticó:
     * WEB, MOBILE o CAJERO.
     */
    public String generarToken(
            String username,
            String rol,
            Long cuentaIdLegacy,
            String canal
    ) {

        return Jwts.builder()

                .subject(username)

                .claim(
                        "rol",
                        rol
                )

                .claim(
                        "cuentaIdLegacy",
                        cuentaIdLegacy
                )

                .claim(
                        "canal",
                        canal
                )

                .issuedAt(
                        new Date()
                )

                .expiration(
                        new Date(
                                System.currentTimeMillis()
                                        + EXPIRACION_MS
                        )
                )

                .signWith(
                        getSigningKey()
                )

                .compact();
    }

    /*
     * Comprueba la firma y vigencia del token.
     */
    public Claims validarToken(String token) {

        return Jwts.parser()

                .verifyWith(
                        getSigningKey()
                )

                .build()

                .parseSignedClaims(token)

                .getPayload();
    }
}
