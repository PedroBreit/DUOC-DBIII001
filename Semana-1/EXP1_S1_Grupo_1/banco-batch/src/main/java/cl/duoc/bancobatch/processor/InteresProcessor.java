package cl.duoc.bancobatch.processor;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

        String nombreNormalizado = item.getNombre().trim();
        String tipoNormalizado = normalizarTipo(item.getTipo());

        BigDecimal tasa = obtenerTasa(tipoNormalizado);

        BigDecimal interes = item.getSaldo()
                .multiply(tasa)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal saldoFinal = item.getSaldo()
                .add(interes)
                .setScale(2, RoundingMode.HALF_UP);

        return new InteresCalculado(
                item.getCuentaId(),
                nombreNormalizado,
                item.getSaldo(),
                item.getEdad(),
                tipoNormalizado,
                tasa,
                interes,
                saldoFinal
        );
    }

    private void validarCuenta(InteresCsv item) {

        if (item.getCuentaId() == null || item.getCuentaId() <= 0) {
            throw new DatoInvalidoException(
                    "ID de cuenta invalido: " + item.getCuentaId()
            );
        }

        if (item.getNombre() == null || item.getNombre().isBlank()) {
            throw new DatoInvalidoException(
                    "Nombre vacio en cuenta ID " + item.getCuentaId()
            );
        }

        if (item.getSaldo() == null) {
            throw new DatoInvalidoException(
                    "Saldo nulo en cuenta ID " + item.getCuentaId()
            );
        }

        if (item.getSaldo().compareTo(BigDecimal.ZERO) <= 0) {
            throw new DatoInvalidoException(
                    "Saldo debe ser mayor que cero. Cuenta ID: "
                            + item.getCuentaId()
                            + ", saldo: "
                            + item.getSaldo()
            );
        }

        if (item.getEdad() == null) {
            throw new DatoInvalidoException(
                    "Edad nula en cuenta ID " + item.getCuentaId()
            );
        }

        if (item.getEdad() < 0 || item.getEdad() > 120) {
            throw new DatoInvalidoException(
                    "Edad fuera de rango. Cuenta ID: "
                            + item.getCuentaId()
                            + ", edad: "
                            + item.getEdad()
            );
        }
    }

    private String normalizarTipo(String tipo) {

        if (tipo == null || tipo.isBlank()) {
            throw new DatoInvalidoException(
                    "Tipo de cuenta vacio"
            );
        }

        String tipoNormalizado =
                tipo.trim().toLowerCase(Locale.ROOT);

        if (!tipoNormalizado.equals("ahorro")
                && !tipoNormalizado.equals("prestamo")) {

            throw new DatoInvalidoException(
                    "Tipo de cuenta no permitido: " + tipo
            );
        }

        return tipoNormalizado;
    }

    private BigDecimal obtenerTasa(String tipo) {

        return switch (tipo) {
            case "ahorro" -> tasaAhorro;
            case "prestamo" -> tasaPrestamo;
            default -> throw new DatoInvalidoException(
                    "No existe tasa configurada para: " + tipo
            );
        };
    }
}
