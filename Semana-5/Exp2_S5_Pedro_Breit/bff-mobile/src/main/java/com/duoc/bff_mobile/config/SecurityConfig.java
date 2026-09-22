package com.duoc.bff_mobile.config;

import com.duoc.bff_mobile.security.JwtAuthFilter;
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
     * Configura la seguridad HTTP del BFF Mobile.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http

            /*
             * Como trabajamos con una API REST y JWT,
             * no utilizamos protección CSRF basada en sesión.
             */
            .csrf(csrf -> csrf.disable())


            /*
             * El servidor no mantiene sesiones.
             * Cada petición debe enviar su JWT.
             */
            .sessionManagement(session ->
                    session.sessionCreationPolicy(
                            SessionCreationPolicy.STATELESS
                    )
            )


            /*
             * Reglas generales de acceso.
             */
            .authorizeHttpRequests(auth -> auth

                    /*
                     * Permitimos la ruta interna de errores.
                     */
                    .requestMatchers("/error")
                    .permitAll()


                    /*
                     * Los endpoints de cuentas Mobile
                     * requieren un usuario con rol CLIENTE
                     * o EMPLEADO.
                     */
                    .requestMatchers(
                            HttpMethod.GET,
                            "/bff-mobile/cuentas/**"
                    )
                    .hasAnyRole(
                            "CLIENTE",
                            "EMPLEADO"
                    )


                    /*
                     * Cualquier otra ruta requiere
                     * autenticación.
                     */
                    .anyRequest()
                    .authenticated()
            )


            /*
             * Respuesta personalizada cuando no existe
             * autenticación válida.
             *
             * Resultado HTTP 401.
             */
            .exceptionHandling(exception -> exception

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
                     * Respuesta personalizada cuando el usuario
                     * está autenticado pero no tiene permiso.
                     *
                     * Resultado HTTP 403.
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
             * Ejecutamos nuestro filtro JWT antes del
             * filtro estándar de autenticación.
             */
            .addFilterBefore(
                    jwtAuthFilter,
                    UsernamePasswordAuthenticationFilter.class
            );


        return http.build();
    }
}
