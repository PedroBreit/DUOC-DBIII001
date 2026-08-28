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
import cl.duoc.bancobatch.model.Transaccion;
import cl.duoc.bancobatch.model.TransaccionCsv;

@Component
public class TransaccionProcessor
        implements ItemProcessor<TransaccionCsv, Transaccion> {

    private static final List<DateTimeFormatter> FORMATOS_FECHA =
            List.of(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                    DateTimeFormatter.ofPattern("yyyy/MM/dd"),
                    DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                    DateTimeFormatter.ofPattern("dd/MM/yyyy")
            );

    @Override
    public Transaccion process(TransaccionCsv item) {

        validarId(item);

        LocalDate fechaNormalizada =
                convertirFecha(item.getFecha());

        BigDecimal montoNormalizado =
                normalizarMonto(item);

        String tipoNormalizado =
                normalizarTipo(item.getTipo());

        return new Transaccion(
                item.getId(),
                fechaNormalizada,
                montoNormalizado,
                tipoNormalizado
        );
    }

    private void validarId(TransaccionCsv item) {

        if (item.getId() == null || item.getId() <= 0) {
            throw new DatoInvalidoException(
                    "ID de transaccion invalido: "
                            + item.getId()
            );
        }
    }

    private BigDecimal normalizarMonto(
            TransaccionCsv item) {

        BigDecimal monto = item.getMonto();

        if (monto == null) {
            throw new DatoInvalidoException(
                    "Monto nulo en transaccion ID "
                            + item.getId()
            );
        }

        if (monto.compareTo(BigDecimal.ZERO) == 0) {
            throw new DatoInvalidoException(
                    "Monto no puede ser cero. ID: "
                            + item.getId()
            );
        }

        return monto.abs();
    }

    private String normalizarTipo(String tipo) {

        if (tipo == null || tipo.isBlank()) {
            throw new DatoInvalidoException(
                    "Tipo de transaccion vacio"
            );
        }

        String tipoNormalizado =
                quitarAcentos(tipo)
                        .trim()
                        .toLowerCase(Locale.ROOT);

        if (!tipoNormalizado.equals("debito")
                && !tipoNormalizado.equals("credito")) {

            throw new DatoInvalidoException(
                    "Tipo de transaccion invalido: "
                            + tipo
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

        for (DateTimeFormatter formato : FORMATOS_FECHA) {
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

    private String quitarAcentos(String valor) {

        return Normalizer
                .normalize(
                        valor,
                        Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}", "");
    }
}
