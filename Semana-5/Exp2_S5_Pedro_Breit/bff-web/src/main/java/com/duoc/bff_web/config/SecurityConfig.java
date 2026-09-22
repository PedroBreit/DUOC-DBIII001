package com.duoc.bff_web.config;

import com.duoc.bff_web.security.JwtAuthFilter;
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

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http

                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers("/error")
                        .permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/bff-web/cuentas/**"
                        )
                        .hasAnyRole(
                                "CLIENTE",
                                "EMPLEADO"
                        )

                        .anyRequest()
                        .authenticated()
                )

                .exceptionHandling(exception ->
                        exception

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

                .addFilterBefore(
                        jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
