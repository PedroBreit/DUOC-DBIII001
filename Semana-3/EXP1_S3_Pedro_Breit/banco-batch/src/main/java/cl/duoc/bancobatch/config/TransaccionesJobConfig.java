package cl.duoc.bancobatch.config;

import javax.sql.DataSource;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.JdbcCursorItemReader;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.infrastructure.item.support.builder.SynchronizedItemStreamReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import cl.duoc.bancobatch.listener.BatchSkipListener;
import cl.duoc.bancobatch.listener.BatchStepListener;
import cl.duoc.bancobatch.model.ResumenTransaccionDiaria;
import cl.duoc.bancobatch.model.Transaccion;
import cl.duoc.bancobatch.model.TransaccionCsv;
import cl.duoc.bancobatch.policy.BancoSkipPolicy;
import cl.duoc.bancobatch.processor.TransaccionProcessor;

@Configuration
public class TransaccionesJobConfig {

    @Bean
    public FlatFileItemReader<TransaccionCsv> transaccionReader() {

        return new FlatFileItemReaderBuilder<TransaccionCsv>()
                .name("transaccionReader")
                .resource(new ClassPathResource("input/transacciones.csv"))
                .encoding("UTF-8")
                .linesToSkip(1)
                .delimited(config -> config
                        .delimiter(",")
                        .names(
                                "id",
                                "fecha",
                                "monto",
                                "tipo"
                        ))
                .targetType(TransaccionCsv.class)
                .build();
    }

    @Bean
    public SynchronizedItemStreamReader<TransaccionCsv> synchronizedTransaccionReader(
            FlatFileItemReader<TransaccionCsv> transaccionReader) {

        return new SynchronizedItemStreamReaderBuilder<TransaccionCsv>()
                .delegate(transaccionReader)
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<Transaccion> transaccionWriter(
            DataSource dataSource) {

        return new JdbcBatchItemWriterBuilder<Transaccion>()
                .dataSource(dataSource)
                .sql("""
                        INSERT INTO transacciones_procesadas
                            (
                                id,
                                fecha,
                                monto,
                                tipo
                            )
                        VALUES
                            (
                                :id,
                                :fecha,
                                :monto,
                                :tipo
                            )
                        ON CONFLICT (id)
                        DO UPDATE SET
                            fecha = EXCLUDED.fecha,
                            monto = EXCLUDED.monto,
                            tipo = EXCLUDED.tipo
                        """)
                .beanMapped()
                .build();
    }

    @Bean
    public JdbcCursorItemReader<ResumenTransaccionDiaria> resumenTransaccionDiariaReader(
            DataSource dataSource) {

        return new JdbcCursorItemReaderBuilder<ResumenTransaccionDiaria>()
                .name("resumenTransaccionDiariaReader")
                .dataSource(dataSource)
                .sql("""
                        SELECT
                            fecha,
                            COUNT(*) AS cantidad_transacciones,

                            COALESCE(
                                SUM(
                                    CASE
                                        WHEN tipo = 'debito'
                                        THEN monto
                                        ELSE 0
                                    END
                                ),
                                0
                            ) AS total_debitos,

                            COALESCE(
                                SUM(
                                    CASE
                                        WHEN tipo = 'credito'
                                        THEN monto
                                        ELSE 0
                                    END
                                ),
                                0
                            ) AS total_creditos,

                            COALESCE(SUM(monto), 0) AS monto_total

                        FROM transacciones_procesadas
                        GROUP BY fecha
                        ORDER BY fecha
                        """)
                .rowMapper((rs, rowNum) ->
                        new ResumenTransaccionDiaria(
                                rs.getDate("fecha").toLocalDate(),
                                rs.getInt("cantidad_transacciones"),
                                rs.getBigDecimal("total_debitos"),
                                rs.getBigDecimal("total_creditos"),
                                rs.getBigDecimal("monto_total")
                        ))
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<ResumenTransaccionDiaria> resumenTransaccionDiariaWriter(
            DataSource dataSource) {

        return new JdbcBatchItemWriterBuilder<ResumenTransaccionDiaria>()
                .dataSource(dataSource)
                .sql("""
                        INSERT INTO resumen_transacciones_diarias
                            (
                                fecha,
                                cantidad_transacciones,
                                total_debitos,
                                total_creditos,
                                monto_total
                            )
                        VALUES
                            (
                                :fecha,
                                :cantidadTransacciones,
                                :totalDebitos,
                                :totalCreditos,
                                :montoTotal
                            )
                        ON CONFLICT (fecha)
                        DO UPDATE SET
                            cantidad_transacciones =
                                EXCLUDED.cantidad_transacciones,
                            total_debitos =
                                EXCLUDED.total_debitos,
                            total_creditos =
                                EXCLUDED.total_creditos,
                            monto_total =
                                EXCLUDED.monto_total
                        """)
                .beanMapped()
                .build();
    }

    @Bean
    public Step procesarTransaccionesStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            SynchronizedItemStreamReader<TransaccionCsv> synchronizedTransaccionReader,
            TransaccionProcessor transaccionProcessor,
            JdbcBatchItemWriter<Transaccion> transaccionWriter,
            BatchSkipListener batchSkipListener,
            BatchStepListener batchStepListener,
            AsyncTaskExecutor transaccionesTaskExecutor,
            @Value("${banco.batch.chunk-size:5}") int chunkSize,
            BancoSkipPolicy bancoSkipPolicy,
            RetryPolicy bancoRetryPolicy) {

        return new StepBuilder(
                "procesarTransaccionesStep",
                jobRepository)
                .<TransaccionCsv, Transaccion>chunk(chunkSize)
                .transactionManager(transactionManager)
                .reader(synchronizedTransaccionReader)
                .processor(transaccionProcessor)
                .writer(transaccionWriter)

                .faultTolerant()
                .skipPolicy(bancoSkipPolicy)
                .retryPolicy(bancoRetryPolicy)

                .skipListener(batchSkipListener)
                .taskExecutor(transaccionesTaskExecutor)
                .listener(batchStepListener)

                .build();
    }

    @Bean
    public Step generarResumenTransaccionesStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            JdbcCursorItemReader<ResumenTransaccionDiaria> resumenTransaccionDiariaReader,
            JdbcBatchItemWriter<ResumenTransaccionDiaria> resumenTransaccionDiariaWriter,
            BatchStepListener batchStepListener,
            @Value("${banco.batch.chunk-size:5}") int chunkSize) {

        return new StepBuilder(
                "generarResumenTransaccionesStep",
                jobRepository)
                .<ResumenTransaccionDiaria, ResumenTransaccionDiaria>chunk(chunkSize)
                .transactionManager(transactionManager)
                .reader(resumenTransaccionDiariaReader)
                .writer(resumenTransaccionDiariaWriter)
                .listener(batchStepListener)
                .build();
    }

    @Bean
    public Job transaccionesJob(
            JobRepository jobRepository,
            Step procesarTransaccionesStep,
            Step generarResumenTransaccionesStep) {

        return new JobBuilder(
                "transaccionesJob",
                jobRepository)
                .start(procesarTransaccionesStep)
                .next(generarResumenTransaccionesStep)
                .build();
    }

    @Bean
    public AsyncTaskExecutor transaccionesTaskExecutor(
            @Value("${banco.batch.threads:3}") int threads,
            @Value("${banco.batch.queue-capacity:10}") int queueCapacity) {

        ThreadPoolTaskExecutor taskExecutor =
                new ThreadPoolTaskExecutor();

        taskExecutor.setCorePoolSize(threads);
        taskExecutor.setMaxPoolSize(threads);
        taskExecutor.setQueueCapacity(queueCapacity);
        taskExecutor.setThreadNamePrefix("transacciones-");

        taskExecutor.initialize();

        return taskExecutor;
    }
}
