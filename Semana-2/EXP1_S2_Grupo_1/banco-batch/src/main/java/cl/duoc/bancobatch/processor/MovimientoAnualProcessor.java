package cl.duoc.bancobatch.processor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import cl.duoc.bancobatch.exception.DatoInvalidoException;
import cl.duoc.bancobatch.model.MovimientoAnual;
import cl.duoc.bancobatch.model.MovimientoAnualCsv;

@Component
public class MovimientoAnualProcessor
        implements ItemProcessor<MovimientoAnualCsv, MovimientoAnual> {

    private static final DateTimeFormatter FORMATO_ESTANDAR =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final DateTimeFormatter FORMATO_LEGACY =
            DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @Override
    public MovimientoAnual process(MovimientoAnualCsv item) {

        validarCuenta(item);
        validarMonto(item);

        LocalDate fechaNormalizada = convertirFecha(item.getFecha());
        String transaccionNormalizada =
                normalizarTransaccion(item.getTransaccion());

        validarSignoMonto(
                transaccionNormalizada,
                item.getMonto(),
                item.getCuentaId()
        );

        String descripcionNormalizada =
                normalizarDescripcion(item.getDescripcion());

        return new MovimientoAnual(
                item.getCuentaId(),
                fechaNormalizada,
                transaccionNormalizada,
                item.getMonto(),
                descripcionNormalizada
        );
    }

    private void validarCuenta(MovimientoAnualCsv item) {

        if (item.getCuentaId() == null || item.getCuentaId() <= 0) {
            throw new DatoInvalidoException(
                    "ID de cuenta invalido: " + item.getCuentaId()
            );
        }
    }

    private void validarMonto(MovimientoAnualCsv item) {

        if (item.getMonto() == null) {
            throw new DatoInvalidoException(
                    "Monto nulo en cuenta ID " + item.getCuentaId()
            );
        }

        if (item.getMonto().compareTo(BigDecimal.ZERO) == 0) {
            throw new DatoInvalidoException(
                    "Monto no puede ser cero. Cuenta ID: "
                            + item.getCuentaId()
            );
        }
    }

    private String normalizarTransaccion(String transaccion) {

        if (transaccion == null || transaccion.isBlank()) {
            throw new DatoInvalidoException(
                    "Tipo de transaccion vacio"
            );
        }

        String valor =
                transaccion.trim().toLowerCase(Locale.ROOT);

        if (!valor.equals("deposito")
                && !valor.equals("retiro")
                && !valor.equals("compra")) {

            throw new DatoInvalidoException(
                    "Tipo de transaccion no permitido: "
                            + transaccion
            );
        }

        return valor;
    }

    private void validarSignoMonto(
            String transaccion,
            BigDecimal monto,
            Long cuentaId) {

        if (transaccion.equals("deposito")
                && monto.compareTo(BigDecimal.ZERO) <= 0) {

            throw new DatoInvalidoException(
                    "Deposito debe tener monto positivo. Cuenta ID: "
                            + cuentaId
            );
        }

        if ((transaccion.equals("retiro")
                || transaccion.equals("compra"))
                && monto.compareTo(BigDecimal.ZERO) >= 0) {

            throw new DatoInvalidoException(
                    "Retiro o compra debe tener monto negativo. Cuenta ID: "
                            + cuentaId
            );
        }
    }

    private String normalizarDescripcion(String descripcion) {

        if (descripcion == null || descripcion.isBlank()) {
            return "SIN DESCRIPCION";
        }

        return descripcion.trim();
    }

    private LocalDate convertirFecha(String fecha) {

        if (fecha == null || fecha.isBlank()) {
            throw new DatoInvalidoException(
                    "Fecha de movimiento vacia"
            );
        }

        String fechaLimpia = fecha.trim();

        try {
            return LocalDate.parse(
                    fechaLimpia,
                    FORMATO_ESTANDAR
            );

        } catch (DateTimeParseException e) {

            try {
                return LocalDate.parse(
                        fechaLimpia,
                        FORMATO_LEGACY
                );

            } catch (DateTimeParseException ex) {
                throw new DatoInvalidoException(
                        "Formato de fecha invalido: " + fecha,
                        ex
                );
            }
        }
    }
}
