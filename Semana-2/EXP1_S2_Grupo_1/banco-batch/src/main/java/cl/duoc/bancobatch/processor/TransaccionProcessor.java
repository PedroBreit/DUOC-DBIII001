package cl.duoc.bancobatch.processor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import cl.duoc.bancobatch.exception.DatoInvalidoException;
import cl.duoc.bancobatch.model.Transaccion;
import cl.duoc.bancobatch.model.TransaccionCsv;

@Component
public class TransaccionProcessor
        implements ItemProcessor<TransaccionCsv, Transaccion> {

    private static final DateTimeFormatter FORMATO_ESTANDAR =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final DateTimeFormatter FORMATO_LEGACY =
            DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @Override
    public Transaccion process(TransaccionCsv item) {

        validarId(item);
        validarMonto(item);

        LocalDate fechaNormalizada = convertirFecha(item.getFecha());
        String tipoNormalizado = normalizarTipo(item.getTipo());

        return new Transaccion(
                item.getId(),
                fechaNormalizada,
                item.getMonto(),
                tipoNormalizado
        );
    }

    private void validarId(TransaccionCsv item) {

        if (item.getId() == null || item.getId() <= 0) {
            throw new DatoInvalidoException(
                    "ID de transaccion invalido: " + item.getId()
            );
        }
    }

    private void validarMonto(TransaccionCsv item) {

        if (item.getMonto() == null) {
            throw new DatoInvalidoException(
                    "Monto nulo en transaccion ID " + item.getId()
            );
        }

        if (item.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new DatoInvalidoException(
                    "Monto debe ser mayor que cero. ID: "
                            + item.getId()
                            + ", monto: "
                            + item.getMonto()
            );
        }
    }

    private String normalizarTipo(String tipo) {

        if (tipo == null || tipo.isBlank()) {
            throw new DatoInvalidoException(
                    "Tipo de transaccion vacio"
            );
        }

        String tipoNormalizado =
                tipo.trim().toLowerCase(Locale.ROOT);

        if (!tipoNormalizado.equals("debito")
                && !tipoNormalizado.equals("credito")) {

            throw new DatoInvalidoException(
                    "Tipo de transaccion invalido: " + tipo
            );
        }

        return tipoNormalizado;
    }

    private LocalDate convertirFecha(String fecha) {

        if (fecha == null || fecha.isBlank()) {
            throw new DatoInvalidoException(
                    "Fecha de transaccion vacia"
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
