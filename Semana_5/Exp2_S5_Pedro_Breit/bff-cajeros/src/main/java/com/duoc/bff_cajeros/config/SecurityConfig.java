package com.duoc.bff_cajeros.config;

import com.duoc.bff_cajeros.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.charset.StandardCharsets;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;


    /*
     * Configuración de seguridad del BFF Cajero.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http

            /*
             * API REST con JWT.
             * No utilizamos sesiones ni formularios.
             */
            .csrf(csrf -> csrf.disable())


            /*
             * Cada petición debe autenticarse con JWT.
             */
            .sessionManagement(session ->
                    session.sessionCreationPolicy(
                            SessionCreationPolicy.STATELESS
                    )
            )


            /*
             * Reglas de acceso por URL.
             */
            .authorizeHttpRequests(auth -> auth

                    /*
                     * Permitimos la ruta interna de errores.
                     */
                    .requestMatchers("/error")
                    .permitAll()


                    /*
                     * Los endpoints del Cajero requieren
                     * rol CLIENTE o EMPLEADO.
                     *
                     * El canal CAJERO se validará además
                     * en el Controller.
                     */
                    .requestMatchers(
                            HttpMethod.GET,
                            "/bff-cajero/cuentas/**"
                    )
                    .hasAnyRole(
                            "CLIENTE",
                            "EMPLEADO"
                    )


                    /*
                     * Cualquier otro endpoint requiere
                     * autenticación.
                     */
                    .anyRequest()
                    .authenticated()
            )


            /*
             * Manejo personalizado de errores 401 y 403.
             */
            .exceptionHandling(exception -> exception

                    /*
                     * No existe autenticación válida.
                     */
                    .authenticationEntryPoint(
                            (request, response, authException) -> {

                                response.setStatus(
                                        HttpServletResponse.SC_UNAUTHORIZED
                                );

                                response.setContentType(
                                        MediaType.APPLICATION_JSON_VALUE
                                );

                                response.setCharacterEncoding(
                                        StandardCharsets.UTF_8.name()
                                );

                                response.getWriter().write(
                                        "{\"mensaje\":\"Debes iniciar sesión para acceder a este recurso\"}"
                                );
                            }
                    )


                    /*
                     * Está autenticado, pero no tiene permiso.
                     */
                    .accessDeniedHandler(
                            (request, response, accessDeniedException) -> {

                                response.setStatus(
                                        HttpServletResponse.SC_FORBIDDEN
                                );

                                response.setContentType(
                                        MediaType.APPLICATION_JSON_VALUE
                                );

                                response.setCharacterEncoding(
                                        StandardCharsets.UTF_8.name()
                                );

                                response.getWriter().write(
                                        "{\"mensaje\":\"No tienes permisos para acceder a este recurso\"}"
                                );
                            }
                    )
            )


            /*
             * Nuestro filtro JWT se ejecuta antes
             * del filtro estándar de Spring Security.
             */
            .addFilterBefore(
                    jwtAuthFilter,
                    UsernamePasswordAuthenticationFilter.class
            );


        return http.build();
    }
}
