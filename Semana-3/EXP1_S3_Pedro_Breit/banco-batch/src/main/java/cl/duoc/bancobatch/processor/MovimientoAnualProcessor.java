package cl.duoc.bancobatch.processor;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import cl.duoc.bancobatch.exception.DatoInvalidoException;
import cl.duoc.bancobatch.model.MovimientoAnual;
import cl.duoc.bancobatch.model.MovimientoAnualCsv;

@Component
public class MovimientoAnualProcessor
        implements ItemProcessor<MovimientoAnualCsv, MovimientoAnual> {

    private static final List<DateTimeFormatter> FORMATOS_FECHA =
            List.of(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                    DateTimeFormatter.ofPattern("yyyy/MM/dd"),
                    DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                    DateTimeFormatter.ofPattern("dd/MM/yyyy")
            );

    @Override
    public MovimientoAnual process(MovimientoAnualCsv item) {

        validarCuenta(item);

        LocalDate fechaNormalizada =
                convertirFecha(item.getFecha());

        String transaccionNormalizada =
                normalizarTransaccion(item.getTransaccion());

        BigDecimal montoNormalizado =
                normalizarMonto(
                        item.getMonto(),
                        transaccionNormalizada,
                        item.getCuentaId()
                );

        String descripcionNormalizada =
                normalizarDescripcion(item.getDescripcion());

        return new MovimientoAnual(
                item.getCuentaId(),
                fechaNormalizada,
                transaccionNormalizada,
                montoNormalizado,
                descripcionNormalizada
        );
    }

    private void validarCuenta(MovimientoAnualCsv item) {

        if (item.getCuentaId() == null
                || item.getCuentaId() <= 0) {

            throw new DatoInvalidoException(
                    "ID de cuenta invalido: "
                            + item.getCuentaId()
            );
        }
    }

    private String normalizarTransaccion(
            String transaccion) {

        if (transaccion == null
                || transaccion.isBlank()) {

            throw new DatoInvalidoException(
                    "Tipo de transaccion vacio"
            );
        }

        String valor =
                quitarAcentos(transaccion)
                        .trim()
                        .toLowerCase(Locale.ROOT);

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

    private BigDecimal normalizarMonto(
            BigDecimal monto,
            String transaccion,
            Long cuentaId) {

        if (monto == null) {
            throw new DatoInvalidoException(
                    "Monto nulo en cuenta ID "
                            + cuentaId
            );
        }

        if (monto.compareTo(BigDecimal.ZERO) == 0) {
            throw new DatoInvalidoException(
                    "Monto no puede ser cero. Cuenta ID: "
                            + cuentaId
            );
        }

        BigDecimal magnitud = monto.abs();

        if (transaccion.equals("deposito")) {
            return magnitud;
        }

        return magnitud.negate();
    }

    private String normalizarDescripcion(
            String descripcion) {

        if (descripcion == null
                || descripcion.isBlank()) {

            return "SIN DESCRIPCION";
        }

        return descripcion.trim();
    }

    private LocalDate convertirFecha(
            String fecha) {

        if (fecha == null
                || fecha.isBlank()) {

            throw new DatoInvalidoException(
                    "Fecha de movimiento vacia"
            );
        }

        String fechaLimpia = fecha.trim();

        for (DateTimeFormatter formato
                : FORMATOS_FECHA) {

            try {
                return LocalDate.parse(
                        fechaLimpia,
                        formato
                );

            } catch (DateTimeParseException ignored) {
                // Se prueba el siguiente formato.
            }
        }

        throw new DatoInvalidoException(
                "Formato de fecha invalido: "
                        + fecha
        );
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
