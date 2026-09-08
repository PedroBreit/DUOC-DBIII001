package com.duoc.bff_cajeros.security;

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
     * Este filtro se ejecuta una vez por cada petición HTTP.
     *
     * Busca el token enviado en:
     *
     * Authorization: Bearer <token>
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader("Authorization");


        /*
         * Si no viene un token Bearer,
         * continuamos sin autenticar.
         *
         * Spring Security responderá 401
         * si el endpoint está protegido.
         */
        if (authorizationHeader == null ||
                !authorizationHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }


        /*
         * Eliminamos el texto "Bearer "
         * y dejamos solamente el JWT.
         */
        String token =
                authorizationHeader.substring(7);


        try {

            /*
             * Validamos firma y expiración.
             */
            Claims claims =
                    jwtService.validarToken(token);


            /*
             * Leemos los datos que Banco Central
             * guardó dentro del JWT.
             */
            String username =
                    claims.getSubject();

            String rol =
                    claims.get("rol", String.class);

            String canal =
                    claims.get("canal", String.class);


            /*
             * Si tenemos todos los datos necesarios,
             * creamos la autenticación de Spring.
             */
            if (username != null &&
                    rol != null &&
                    canal != null &&
                    SecurityContextHolder
                            .getContext()
                            .getAuthentication() == null) {


                /*
                 * CLIENTE -> ROLE_CLIENTE
                 * EMPLEADO -> ROLE_EMPLEADO
                 */
                SimpleGrantedAuthority autoridadRol =
                        new SimpleGrantedAuthority(
                                "ROLE_" + rol.toUpperCase()
                        );


                /*
                 * CAJERO -> CANAL_CAJERO
                 * WEB    -> CANAL_WEB
                 * MOBILE -> CANAL_MOBILE
                 */
                SimpleGrantedAuthority autoridadCanal =
                        new SimpleGrantedAuthority(
                                "CANAL_" + canal.toUpperCase()
                        );


                /*
                 * Creamos el usuario autenticado
                 * para esta petición.
                 */
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                List.of(
                                        autoridadRol,
                                        autoridadCanal
                                )
                        );


                /*
                 * Guardamos la autenticación dentro
                 * del contexto de Spring Security.
                 */
                SecurityContextHolder
                        .getContext()
                        .setAuthentication(authentication);
            }

        } catch (JwtException | IllegalArgumentException e) {

            /*
             * Token vencido, modificado o con
             * una firma incorrecta.
             */
            SecurityContextHolder.clearContext();
        }


        /*
         * Continuamos con el resto de filtros.
         */
        filterChain.doFilter(request, response);
    }
}
