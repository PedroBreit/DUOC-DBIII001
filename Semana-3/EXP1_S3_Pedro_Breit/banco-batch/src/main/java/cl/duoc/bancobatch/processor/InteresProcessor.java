package cl.duoc.bancobatch.processor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.Locale;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import cl.duoc.bancobatch.exception.DatoInvalidoException;
import cl.duoc.bancobatch.model.InteresCalculado;
import cl.duoc.bancobatch.model.InteresCsv;

@Component
public class InteresProcessor
        implements ItemProcessor<InteresCsv, InteresCalculado> {

    private final BigDecimal tasaAhorro;
    private final BigDecimal tasaPrestamo;

    public InteresProcessor(
            @Value("${banco.interes.ahorro}") BigDecimal tasaAhorro,
            @Value("${banco.interes.prestamo}") BigDecimal tasaPrestamo) {

        this.tasaAhorro = tasaAhorro;
        this.tasaPrestamo = tasaPrestamo;
    }

    @Override
    public InteresCalculado process(InteresCsv item) {

        validarCuenta(item);

        String nombreNormalizado =
                normalizarNombre(
                        item.getNombre(),
                        item.getCuentaId()
                );

        String tipoNormalizado =
                normalizarTipo(item.getTipo());

        BigDecimal saldoNormalizado =
                normalizarSaldo(
                        item.getSaldo(),
                        item.getCuentaId()
                );

        Integer edadNormalizada =
                validarEdad(
                        item.getEdad(),
                        item.getCuentaId()
                );

        BigDecimal tasa =
                obtenerTasa(tipoNormalizado);

        BigDecimal interes =
                saldoNormalizado
                        .multiply(tasa)
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        BigDecimal saldoFinal =
                saldoNormalizado
                        .add(interes)
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        return new InteresCalculado(
                item.getCuentaId(),
                nombreNormalizado,
                saldoNormalizado,
                edadNormalizada,
                tipoNormalizado,
                tasa,
                interes,
                saldoFinal
        );
    }

    private void validarCuenta(InteresCsv item) {

        if (item.getCuentaId() == null
                || item.getCuentaId() <= 0) {

            throw new DatoInvalidoException(
                    "ID de cuenta invalido: "
                            + item.getCuentaId()
            );
        }
    }

    private String normalizarNombre(
            String nombre,
            Long cuentaId) {

        if (nombre == null
                || nombre.isBlank()) {

            throw new DatoInvalidoException(
                    "Nombre vacio en cuenta ID "
                            + cuentaId
            );
        }

        String nombreNormalizado =
                nombre.trim();

        if (nombreNormalizado.equalsIgnoreCase("unknown")) {

            throw new DatoInvalidoException(
                    "Nombre desconocido en cuenta ID "
                            + cuentaId
            );
        }

        return nombreNormalizado;
    }

    private BigDecimal normalizarSaldo(
            BigDecimal saldo,
            Long cuentaId) {

        if (saldo == null) {

            throw new DatoInvalidoException(
                    "Saldo nulo en cuenta ID "
                            + cuentaId
            );
        }

        if (saldo.compareTo(BigDecimal.ZERO) <= 0) {

            throw new DatoInvalidoException(
                    "Saldo debe ser mayor que cero. Cuenta ID: "
                            + cuentaId
                            + ", saldo: "
                            + saldo
            );
        }

        return saldo;
    }

    private Integer validarEdad(
            Integer edad,
            Long cuentaId) {

        if (edad == null) {

            throw new DatoInvalidoException(
                    "Edad nula en cuenta ID "
                            + cuentaId
            );
        }

        if (edad < 0 || edad > 120) {

            throw new DatoInvalidoException(
                    "Edad fuera de rango. Cuenta ID: "
                            + cuentaId
                            + ", edad: "
                            + edad
            );
        }

        return edad;
    }

    private String normalizarTipo(String tipo) {

        if (tipo == null || tipo.isBlank()) {

            throw new DatoInvalidoException(
                    "Tipo de cuenta vacio"
            );
        }

        String tipoNormalizado =
                quitarAcentos(tipo)
                        .trim()
                        .toLowerCase(Locale.ROOT);

        if (tipoNormalizado.equals("hipoteca")) {
            return "prestamo";
        }

        if (!tipoNormalizado.equals("ahorro")
                && !tipoNormalizado.equals("prestamo")) {

            throw new DatoInvalidoException(
                    "Tipo de cuenta no permitido: "
                            + tipo
            );
        }

        return tipoNormalizado;
    }

    private BigDecimal obtenerTasa(
            String tipo) {

        return switch (tipo) {

            case "ahorro" ->
                    tasaAhorro;

            case "prestamo" ->
                    tasaPrestamo;

            default ->
                    throw new DatoInvalidoException(
                            "No existe tasa configurada para: "
                                    + tipo
                    );
        };
    }

    private String quitarAcentos(
            String valor) {

        return Normalizer
                .normalize(
                        valor,
                        Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}", "");
    }
}
