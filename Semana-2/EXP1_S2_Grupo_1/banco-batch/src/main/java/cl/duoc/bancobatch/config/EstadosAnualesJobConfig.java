package cl.duoc.bancobatch.config;

import javax.sql.DataSource;

import org.springframework.batch.infrastructure.item.file.FlatFileItemWriter;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.infrastructure.item.support.CompositeItemWriter;
import org.springframework.batch.infrastructure.item.support.builder.CompositeItemWriterBuilder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.batch.infrastructure.item.database.JdbcCursorItemReader;
import org.springframework.batch.infrastructure.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.dao.TransientDataAccessException;

import org.springframework.batch.infrastructure.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.infrastructure.item.support.builder.SynchronizedItemStreamReaderBuilder;

import cl.duoc.bancobatch.listener.BatchStepListener;
import cl.duoc.bancobatch.model.MovimientoAnualCsv;
import cl.duoc.bancobatch.model.MovimientoAnual;
import cl.duoc.bancobatch.exception.DatoInvalidoException;
import cl.duoc.bancobatch.listener.BatchSkipListener;
import cl.duoc.bancobatch.processor.MovimientoAnualProcessor;
import cl.duoc.bancobatch.model.EstadoCuentaAnual;

@Configuration
public class EstadosAnualesJobConfig {

    @Bean
    public FlatFileItemReader<MovimientoAnualCsv> movimientoAnualReader() {

        return new FlatFileItemReaderBuilder<MovimientoAnualCsv>()
                .name("movimientoAnualReader")
                .resource(new ClassPathResource("input/cuentas_anuales.csv"))
                .encoding("UTF-8")
                .linesToSkip(1)
                .delimited(config -> config
                        .delimiter(",")
                        .names(
                                "cuentaId",
                                "fecha",
                                "transaccion",
                                "monto",
                                "descripcion"
                        ))
                .targetType(MovimientoAnualCsv.class)
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<MovimientoAnual> movimientoAnualWriter(
            DataSource dataSource) {

        return new JdbcBatchItemWriterBuilder<MovimientoAnual>()
                .dataSource(dataSource)
                .sql("""
                        INSERT INTO movimientos_anuales
                            (
                                cuenta_id,
                                fecha,
                                transaccion,
                                monto,
                                descripcion
                            )
                        VALUES
                            (
                                :cuentaId,
                                :fecha,
                                :transaccion,
                                :monto,
                                :descripcion
                            )
                        ON CONFLICT ON CONSTRAINT uk_movimiento_anual
                        DO NOTHING
                        """)
                .beanMapped()
                .assertUpdates(false)
                .build();
    }

    @Bean
    public Step procesarMovimientosAnualesStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            SynchronizedItemStreamReader<MovimientoAnualCsv> synchronizedMovimientoAnualReader,
            MovimientoAnualProcessor movimientoAnualProcessor,
            JdbcBatchItemWriter<MovimientoAnual> movimientoAnualWriter,
            BatchSkipListener batchSkipListener,
            BatchStepListener batchStepListener,
            AsyncTaskExecutor estadosAnualesTaskExecutor) {

        return new StepBuilder(
                "procesarMovimientosAnualesStep",
                jobRepository)
                .<MovimientoAnualCsv, MovimientoAnual>chunk(5)
                .transactionManager(transactionManager)
                .reader(synchronizedMovimientoAnualReader)
                .processor(movimientoAnualProcessor)
                .writer(movimientoAnualWriter)

                .faultTolerant()

                .skip(DatoInvalidoException.class)
                .skipLimit(100)

                .retry(TransientDataAccessException.class)
                .retryLimit(3)

                .skipListener(batchSkipListener)
                .taskExecutor(estadosAnualesTaskExecutor)
                .listener(batchStepListener)

                .build();
    }

    @Bean
    public JdbcCursorItemReader<EstadoCuentaAnual> estadoCuentaAnualReader(
            DataSource dataSource) {

        return new JdbcCursorItemReaderBuilder<EstadoCuentaAnual>()
                .name("estadoCuentaAnualReader")
                .dataSource(dataSource)
                .sql("""
                        SELECT
                            cuenta_id,
                            COALESCE(
                                SUM(
                                    CASE
                                        WHEN transaccion = 'deposito'
                                        THEN monto
                                        ELSE 0
                                    END
                                ),
                                0
                            ) AS total_depositos,

                            COALESCE(
                                SUM(
                                    CASE
                                        WHEN transaccion = 'retiro'
                                        THEN monto
                                        ELSE 0
                                    END
                                ),
                                0
                            ) AS total_retiros,

                            COALESCE(
                                SUM(
                                    CASE
                                        WHEN transaccion = 'compra'
                                        THEN monto
                                        ELSE 0
                                    END
                                ),
                                0
                            ) AS total_compras,

                            COALESCE(SUM(monto), 0) AS saldo_neto,

                            COUNT(*) AS cantidad_movimientos

                        FROM movimientos_anuales
                        GROUP BY cuenta_id
                        ORDER BY cuenta_id
                        """)
                .rowMapper((rs, rowNum) ->
                        new EstadoCuentaAnual(
                                rs.getLong("cuenta_id"),
                                rs.getBigDecimal("total_depositos"),
                                rs.getBigDecimal("total_retiros"),
                                rs.getBigDecimal("total_compras"),
                                rs.getBigDecimal("saldo_neto"),
                                rs.getInt("cantidad_movimientos")
                        ))
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<EstadoCuentaAnual> estadoCuentaAnualWriter(
            DataSource dataSource) {

        return new JdbcBatchItemWriterBuilder<EstadoCuentaAnual>()
                .dataSource(dataSource)
                .sql("""
                        INSERT INTO estados_cuenta_anuales
                            (
                                cuenta_id,
                                total_depositos,
                                total_retiros,
                                total_compras,
                                saldo_neto,
                                cantidad_movimientos
                            )
                        VALUES
                            (
                                :cuentaId,
                                :totalDepositos,
                                :totalRetiros,
                                :totalCompras,
                                :saldoNeto,
                                :cantidadMovimientos
                            )
                        ON CONFLICT (cuenta_id)
                        DO UPDATE SET
                            total_depositos =
                                EXCLUDED.total_depositos,
                            total_retiros =
                                EXCLUDED.total_retiros,
                            total_compras =
                                EXCLUDED.total_compras,
                            saldo_neto =
                                EXCLUDED.saldo_neto,
                            cantidad_movimientos =
                                EXCLUDED.cantidad_movimientos
                        """)
                .beanMapped()
                .build();
    }

    @Bean
    public FlatFileItemWriter<EstadoCuentaAnual> estadoCuentaAnualCsvWriter() {

        return new FlatFileItemWriterBuilder<EstadoCuentaAnual>()
                .name("estadoCuentaAnualCsvWriter")
                .resource(
                        new FileSystemResource(
                                "output/estados_cuenta_anuales.csv"
                        )
                )
                .encoding("UTF-8")
                .shouldDeleteIfExists(true)
                .headerCallback(writer ->
                        writer.write(
                                "cuenta_id,total_depositos,total_retiros,"
                                        + "total_compras,saldo_neto,"
                                        + "cantidad_movimientos"
                        )
                )
                .delimited(config -> config
                        .delimiter(",")
                        .names(
                                "cuentaId",
                                "totalDepositos",
                                "totalRetiros",
                                "totalCompras",
                                "saldoNeto",
                                "cantidadMovimientos"
                        ))
                .build();
    }

    @Bean
    public CompositeItemWriter<EstadoCuentaAnual> estadoCuentaAnualCompositeWriter(
            JdbcBatchItemWriter<EstadoCuentaAnual> estadoCuentaAnualWriter,
            FlatFileItemWriter<EstadoCuentaAnual> estadoCuentaAnualCsvWriter) {

        return new CompositeItemWriterBuilder<EstadoCuentaAnual>()
                .delegates(
                        estadoCuentaAnualWriter,
                        estadoCuentaAnualCsvWriter
                )
                .build();
    }

    @Bean
    public Step generarResumenAnualStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            JdbcCursorItemReader<EstadoCuentaAnual> estadoCuentaAnualReader,
            CompositeItemWriter<EstadoCuentaAnual> estadoCuentaAnualCompositeWriter,
            BatchStepListener batchStepListener
        ) {

        return new StepBuilder(
                "generarResumenAnualStep",
                jobRepository)
                .<EstadoCuentaAnual, EstadoCuentaAnual>chunk(5)
                .transactionManager(transactionManager)
                .reader(estadoCuentaAnualReader)
                .writer(estadoCuentaAnualCompositeWriter)
                .listener(batchStepListener)
                .build();
    }

    @Bean
    public Job estadosAnualesJob(
            JobRepository jobRepository,
            Step procesarMovimientosAnualesStep,
            Step generarResumenAnualStep) {

        return new JobBuilder(
                "estadosAnualesJob",
                jobRepository)
                .start(procesarMovimientosAnualesStep)
                .next(generarResumenAnualStep)
                .build();
    }

    @Bean
    public SynchronizedItemStreamReader<MovimientoAnualCsv> synchronizedMovimientoAnualReader(
            FlatFileItemReader<MovimientoAnualCsv> movimientoAnualReader) {

        return new SynchronizedItemStreamReaderBuilder<MovimientoAnualCsv>()
                .delegate(movimientoAnualReader)
                .build();
    }

    @Bean
    public AsyncTaskExecutor estadosAnualesTaskExecutor() {

        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();

        taskExecutor.setCorePoolSize(3);
        taskExecutor.setMaxPoolSize(3);
        taskExecutor.setQueueCapacity(10);
        taskExecutor.setThreadNamePrefix("estados-anuales-");

        taskExecutor.initialize();

        return taskExecutor;
    }
}
