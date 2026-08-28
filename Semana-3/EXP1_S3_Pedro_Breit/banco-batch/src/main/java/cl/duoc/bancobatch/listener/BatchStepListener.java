package cl.duoc.bancobatch.listener;

import java.time.Duration;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.stereotype.Component;

@Component
public class BatchStepListener implements StepExecutionListener {

    private static final Logger logger =
            LoggerFactory.getLogger(BatchStepListener.class);

    private LocalDateTime inicio;

    @Override
    public void beforeStep(StepExecution stepExecution) {

        inicio = LocalDateTime.now();

        logger.info(
                "STEP INICIADO | step={} | fechaInicio={}",
                stepExecution.getStepName(),
                inicio
        );
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {

        LocalDateTime fin = LocalDateTime.now();

        long duracionMs =
                Duration.between(inicio, fin).toMillis();

        logger.info(
                "STEP FINALIZADO | step={} | status={} | leidos={} | escritos={} | omitidosLectura={} | omitidosProceso={} | omitidosEscritura={} | commits={} | rollbacks={} | duracionMs={}",
                stepExecution.getStepName(),
                stepExecution.getStatus(),
                stepExecution.getReadCount(),
                stepExecution.getWriteCount(),
                stepExecution.getReadSkipCount(),
                stepExecution.getProcessSkipCount(),
                stepExecution.getWriteSkipCount(),
                stepExecution.getCommitCount(),
                stepExecution.getRollbackCount(),
                duracionMs
        );

        return stepExecution.getExitStatus();
    }
}
