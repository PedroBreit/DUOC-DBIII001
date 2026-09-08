package com.duoc.banco_central_xyz.service;

import com.duoc.banco_central_xyz.entity.Usuario;
import com.duoc.banco_central_xyz.repository.UsuarioRepository;
import com.duoc.banco_central_xyz.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /*
     * Canales válidos que pueden solicitar autenticación.
     */
    private static final Set<String> CANALES_VALIDOS =
            Set.of(
                    "WEB",
                    "MOBILE",
                    "CAJERO"
            );

    public AuthService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {

        this.usuarioRepository =
                usuarioRepository;

        this.passwordEncoder =
                passwordEncoder;

        this.jwtService =
                jwtService;
    }

    /*
     * Autentica al usuario y genera un JWT asociado
     * específicamente al canal utilizado.
     */
    public String login(
            String username,
            String password,
            String canal
    ) {

        Usuario usuario =
                usuarioRepository
                        .findByUsername(username)
                        .orElseThrow(
                                () -> new BadCredentialsException(
                                        "Usuario o contraseña incorrectos"
                                )
                        );

        /*
         * Verifica la contraseña almacenada mediante BCrypt.
         */
        if (!passwordEncoder.matches(
                password,
                usuario.getPasswordHash()
        )) {

            throw new BadCredentialsException(
                    "Usuario o contraseña incorrectos"
            );
        }

        /*
         * El canal es obligatorio.
         */
        if (canal == null || canal.isBlank()) {

            throw new IllegalArgumentException(
                    "Debes indicar el canal de autenticación"
            );
        }

        String canalNormalizado =
                canal.trim().toUpperCase();

        /*
         * Solo aceptamos los tres canales definidos
         * por la arquitectura BFF.
         */
        if (!CANALES_VALIDOS.contains(
                canalNormalizado
        )) {

            throw new IllegalArgumentException(
                    "Canal de autenticación no válido"
            );
        }

        /*
         * Genera un token que contiene:
         *
         * - username
         * - rol
         * - cuenta asociada
         * - canal
         */
        return jwtService.generarToken(

                usuario.getUsername(),

                usuario.getRol().name(),

                usuario.getCuentaIdLegacy(),

                canalNormalizado
        );
    }
}
