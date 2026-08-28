package cl.duoc.bancobatch.policy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import cl.duoc.bancobatch.exception.DatoInvalidoException;

@Component
public class BancoSkipPolicy implements SkipPolicy {

    private static final Logger log =
            LoggerFactory.getLogger(BancoSkipPolicy.class);

    private final long skipLimit;

    public BancoSkipPolicy(
            @Value("${banco.batch.skip-limit:100}") long skipLimit) {
        this.skipLimit = skipLimit;
    }

    @Override
    public boolean shouldSkip(Throwable throwable, long skipCount)
            throws SkipLimitExceededException {

        if (throwable == null) {
            throw new IllegalArgumentException(
                    "La excepcion no puede ser nula"
            );
        }

        boolean excepcionOmitible =
                throwable instanceof DatoInvalidoException
                || throwable instanceof FlatFileParseException;

        if (!excepcionOmitible) {
            return false;
        }

        // Spring Batch puede consultar la politica con un contador negativo
        // solo para verificar si el tipo de excepcion es omitible.
        if (skipCount < 0) {
            return true;
        }

        if (skipCount >= skipLimit) {

            log.error(
                    "SKIP POLICY | limite excedido={} | excepcion={}",
                    skipLimit,
                    throwable.getClass().getSimpleName()
            );

            throw new SkipLimitExceededException(
                    skipLimit,
                    throwable
            );
        }

        log.warn(
                "SKIP POLICY | registro omitido={} de {} | excepcion={} | motivo={}",
                skipCount + 1,
                skipLimit,
                throwable.getClass().getSimpleName(),
                throwable.getMessage()
        );

        return true;
    }
}
