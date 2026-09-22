package com.duoc.bff_web.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    /*
     * Este filtro se ejecuta una vez por cada
     * petición recibida por el BFF Web.
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader(
                        "Authorization"
                );

        /*
         * Si no existe un Bearer Token,
         * dejamos que Spring Security gestione
         * posteriormente el error 401.
         */
        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }

        String token =
                authorizationHeader.substring(7);

        try {

            /*
             * Valida firma y expiración.
             */
            Claims claims =
                    jwtService.validarToken(token);

            String username =
                    claims.getSubject();

            String rol =
                    claims.get(
                            "rol",
                            String.class
                    );

            String canal =
                    claims.get(
                            "canal",
                            String.class
                    );

            if (username != null
                    && rol != null
                    && canal != null
                    && SecurityContextHolder
                            .getContext()
                            .getAuthentication() == null) {

                /*
                 * Autoridad basada en rol.
                 *
                 * Ejemplo:
                 * ROLE_CLIENTE
                 */
                SimpleGrantedAuthority autoridadRol =
                        new SimpleGrantedAuthority(
                                "ROLE_" + rol
                        );

                /*
                 * Autoridad específica del canal.
                 *
                 * Ejemplo:
                 * CANAL_WEB
                 */
                SimpleGrantedAuthority autoridadCanal =
                        new SimpleGrantedAuthority(
                                "CANAL_" + canal.toUpperCase()
                        );

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(

                                username,

                                null,

                                List.of(
                                        autoridadRol,
                                        autoridadCanal
                                )
                        );

                SecurityContextHolder
                        .getContext()
                        .setAuthentication(
                                authentication
                        );
            }

        } catch (
                JwtException |
                IllegalArgumentException e
        ) {

            /*
             * Un token inválido, alterado o expirado
             * no autentica al usuario.
             */
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(
                request,
                response
        );
    }
}
