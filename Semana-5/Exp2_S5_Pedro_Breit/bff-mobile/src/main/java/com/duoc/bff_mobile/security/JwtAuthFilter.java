package com.duoc.bff_mobile.security;

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
     * Busca el JWT enviado en:
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
         * Si no existe Authorization o no comienza con Bearer,
         * continuamos la petición sin autenticar.
         *
         * Spring Security decidirá posteriormente si el
         * endpoint requiere autenticación.
         */
        if (authorizationHeader == null ||
                !authorizationHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }


        /*
         * Eliminamos "Bearer " para obtener solamente el JWT.
         */
        String token = authorizationHeader.substring(7);


        try {

            /*
             * Validamos el JWT y obtenemos sus claims.
             */
            Claims claims = jwtService.validarToken(token);

            String username = claims.getSubject();

            String rol =
                    claims.get("rol", String.class);

            String canal =
                    claims.get("canal", String.class);


            /*
             * Solo autenticamos si tenemos todos los datos
             * necesarios dentro del JWT.
             */
            if (username != null &&
                    rol != null &&
                    canal != null &&
                    SecurityContextHolder
                            .getContext()
                            .getAuthentication() == null) {


                /*
                 * Convertimos el rol del JWT en una autoridad
                 * compatible con Spring Security.
                 *
                 * CLIENTE -> ROLE_CLIENTE
                 * EMPLEADO -> ROLE_EMPLEADO
                 */
                SimpleGrantedAuthority autoridadRol =
                        new SimpleGrantedAuthority(
                                "ROLE_" + rol.toUpperCase()
                        );


                /*
                 * Convertimos también el canal.
                 *
                 * MOBILE -> CANAL_MOBILE
                 * WEB    -> CANAL_WEB
                 * CAJERO -> CANAL_CAJERO
                 */
                SimpleGrantedAuthority autoridadCanal =
                        new SimpleGrantedAuthority(
                                "CANAL_" + canal.toUpperCase()
                        );


                /*
                 * Creamos la autenticación que utilizará
                 * Spring Security durante esta petición.
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
                 * Guardamos la autenticación en el contexto
                 * de seguridad de Spring.
                 */
                SecurityContextHolder
                        .getContext()
                        .setAuthentication(authentication);
            }

        } catch (JwtException | IllegalArgumentException e) {

            /*
             * Si el token está vencido, modificado o posee
             * una firma incorrecta, eliminamos cualquier
             * autenticación.
             */
            SecurityContextHolder.clearContext();
        }


        /*
         * Continuamos con el resto de filtros.
         */
        filterChain.doFilter(request, response);
    }
}
