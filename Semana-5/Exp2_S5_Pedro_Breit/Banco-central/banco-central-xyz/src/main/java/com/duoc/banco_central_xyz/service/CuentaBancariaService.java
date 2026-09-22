package com.duoc.banco_central_xyz.service;

import com.duoc.banco_central_xyz.dto.CuentaBancariaDto;
import com.duoc.banco_central_xyz.dto.CuentaBancariaSaldoDto;
import com.duoc.banco_central_xyz.dto.RetiroCuentaBancariaRequestDto;
import com.duoc.banco_central_xyz.dto.RetiroCuentaBancariaResponseDto;
import com.duoc.banco_central_xyz.entity.CuentaBancaria;
import com.duoc.banco_central_xyz.entity.Usuario;
import com.duoc.banco_central_xyz.exception.AccesoDenegadoException;
import com.duoc.banco_central_xyz.exception.CuentaNoEncontradaException;
import com.duoc.banco_central_xyz.exception.FondosInsuficientesException;
import com.duoc.banco_central_xyz.exception.OperacionNoPermitidaException;
import com.duoc.banco_central_xyz.mapper.CuentaBancariaMapper;
import com.duoc.banco_central_xyz.repository.CuentaBancariaRepository;
import com.duoc.banco_central_xyz.repository.UsuarioRepository;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
public class CuentaBancariaService {

    private final CuentaBancariaRepository cuentaBancariaRepository;
    private final CuentaBancariaMapper cuentaBancariaMapper;
    private final UsuarioRepository usuarioRepository;


    public CuentaBancariaService(
            CuentaBancariaRepository cuentaBancariaRepository,
            CuentaBancariaMapper cuentaBancariaMapper,
            UsuarioRepository usuarioRepository
    ) {

        this.cuentaBancariaRepository = cuentaBancariaRepository;
        this.cuentaBancariaMapper = cuentaBancariaMapper;
        this.usuarioRepository = usuarioRepository;
    }


    /*
     * Lista todas las cuentas.
     *
     * El Controller se encarga de restringir
     * este endpoint al rol EMPLEADO.
     */
    public List<CuentaBancariaDto> listarTodas() {

        return cuentaBancariaRepository.findAll()
                .stream()
                .map(cuentaBancariaMapper::toDto)
                .toList();
    }


    /*
     * Obtiene la información completa de una cuenta.
     *
     * CLIENTE:
     * solo puede consultar su propia cuenta.
     *
     * EMPLEADO:
     * puede consultar cualquier cuenta.
     */
    public CuentaBancariaDto obtenerPorId(
            Long id,
            String username
    ) {

        CuentaBancaria cuenta =
                obtenerCuenta(id);

        Usuario usuario =
                obtenerUsuario(username);


        validarAccesoConsulta(
                cuenta,
                usuario
        );


        return cuentaBancariaMapper.toDto(cuenta);
    }


    /*
     * Consulta únicamente el saldo final.
     */
    public CuentaBancariaSaldoDto obtenerSaldo(
            Long id,
            String username
    ) {

        CuentaBancaria cuenta =
                obtenerCuenta(id);

        Usuario usuario =
                obtenerUsuario(username);


        validarAccesoConsulta(
                cuenta,
                usuario
        );


        return new CuentaBancariaSaldoDto(
                cuenta.getSaldoFinal()
        );
    }


    /*
     * Realiza un retiro.
     *
     * Esta operación es transaccional:
     * si ocurre un error, el cambio de saldo
     * no queda parcialmente guardado.
     */
    @Transactional
    public RetiroCuentaBancariaResponseDto retirar(
            Long id,
            RetiroCuentaBancariaRequestDto request,
            String username
    ) {

        CuentaBancaria cuenta =
                obtenerCuenta(id);

        Usuario usuario =
                obtenerUsuario(username);


        /*
         * Para retiros exigimos que la cuenta
         * pertenezca directamente al usuario.
         *
         * Un empleado no puede retirar dinero
         * desde la cuenta de otra persona.
         */
        validarPropietario(
                cuenta,
                usuario
        );


        /*
         * Validamos que exista un monto
         * y que sea mayor a cero.
         */
        if (request == null ||
                request.monto() == null ||
                request.monto().compareTo(BigDecimal.ZERO) <= 0) {

            throw new OperacionNoPermitidaException(
                    "El monto de retiro debe ser mayor a cero"
            );
        }


        /*
         * Según la lógica del proyecto,
         * solo las cuentas de ahorro permiten retiro.
         */
        if (!"ahorro".equalsIgnoreCase(
                cuenta.getTipo()
        )) {

            throw new OperacionNoPermitidaException(
                    "Solo se pueden realizar retiros desde cuentas de ahorro"
            );
        }


        BigDecimal saldoInicial =
                cuenta.getSaldoFinal();

        BigDecimal montoRetiro =
                request.monto();


        /*
         * No permitimos retirar más dinero
         * del disponible.
         */
        if (saldoInicial.compareTo(montoRetiro) < 0) {

            throw new FondosInsuficientesException();
        }


        /*
         * Calculamos y guardamos el nuevo saldo.
         */
        BigDecimal nuevoSaldo =
                saldoInicial.subtract(montoRetiro);


        cuenta.setSaldoFinal(nuevoSaldo);

        cuentaBancariaRepository.save(cuenta);


        /*
         * Retornamos solamente información relevante
         * sobre la operación.
         */
        return new RetiroCuentaBancariaResponseDto(
                saldoInicial,
                montoRetiro,
                nuevoSaldo
        );
    }


    /*
     * Busca una cuenta por su ID interno.
     */
    private CuentaBancaria obtenerCuenta(Long id) {

        return cuentaBancariaRepository
                .findById(id)
                .orElseThrow(
                        () -> new CuentaNoEncontradaException(id)
                );
    }


    /*
     * Busca al usuario autenticado.
     */
    private Usuario obtenerUsuario(String username) {

        return usuarioRepository
                .findByUsername(username)
                .orElseThrow(
                        () -> new BadCredentialsException(
                                "Usuario no encontrado"
                        )
                );
    }


    /*
     * Validación utilizada para consultas.
     *
     * EMPLEADO puede consultar cualquier cuenta.
     * CLIENTE solamente su cuenta.
     */
    private void validarAccesoConsulta(
            CuentaBancaria cuenta,
            Usuario usuario
    ) {

        boolean esEmpleado =
                "EMPLEADO".equals(
                        usuario.getRol().name()
                );


        boolean esSuCuenta =
                Objects.equals(
                        cuenta.getCuentaIdLegacy(),
                        usuario.getCuentaIdLegacy()
                );


        if (!esEmpleado && !esSuCuenta) {

            throw new AccesoDenegadoException();
        }
    }


    /*
     * Validación más estricta para operaciones
     * que modifican dinero.
     *
     * El usuario debe ser dueño de la cuenta.
     */
    private void validarPropietario(
            CuentaBancaria cuenta,
            Usuario usuario
    ) {

        boolean esSuCuenta =
                Objects.equals(
                        cuenta.getCuentaIdLegacy(),
                        usuario.getCuentaIdLegacy()
                );


        if (!esSuCuenta) {

            throw new AccesoDenegadoException();
        }
    }
}
