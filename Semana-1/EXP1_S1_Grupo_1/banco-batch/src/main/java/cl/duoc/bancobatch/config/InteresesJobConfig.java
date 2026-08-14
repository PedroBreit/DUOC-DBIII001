package cl.duoc.bancobatch.config;

import javax.sql.DataSource;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

import cl.duoc.bancobatch.model.InteresCsv;
import cl.duoc.bancobatch.model.InteresCalculado;
import cl.duoc.bancobatch.exception.DatoInvalidoException;
import cl.duoc.bancobatch.listener.BatchSkipListener;
import cl.duoc.bancobatch.processor.InteresProcessor;

@Configuration
public class InteresesJobConfig {

    @Bean
    public FlatFileItemReader<InteresCsv> interesReader() {

        return new FlatFileItemReaderBuilder<InteresCsv>()
                .name("interesReader")
                .resource(new ClassPathResource("input/intereses.csv"))
                .encoding("UTF-8")
                .linesToSkip(1)
                .delimited(config -> config
                        .delimiter(",")
                        .names(
                                "cuentaId",
                                "nombre",
                                "saldo",
                                "edad",
                                "tipo"
                        ))
                .targetType(InteresCsv.class)
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<InteresCalculado> interesWriter(
            DataSource dataSource) {

        return new JdbcBatchItemWriterBuilder<InteresCalculado>()
                .dataSource(dataSource)
                .sql("""
                        INSERT INTO intereses_calculados
                            (
                                cuenta_id,
                                nombre,
                                saldo_inicial,
                                edad,
                                tipo,
                                tasa,
                                interes,
                                saldo_final
                            )
                        VALUES
                            (
                                :cuentaId,
                                :nombre,
                                :saldoInicial,
                                :edad,
                                :tipo,
                                :tasa,
                                :interes,
                                :saldoFinal
                            )
                        ON CONFLICT (cuenta_id)
                        DO UPDATE SET
                            nombre = EXCLUDED.nombre,
                            saldo_inicial = EXCLUDED.saldo_inicial,
                            edad = EXCLUDED.edad,
                            tipo = EXCLUDED.tipo,
                            tasa = EXCLUDED.tasa,
                            interes = EXCLUDED.interes,
                            saldo_final = EXCLUDED.saldo_final
                        """)
                .beanMapped()
                .build();
    }

    @Bean
    public Step procesarInteresesStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            FlatFileItemReader<InteresCsv> interesReader,
            InteresProcessor interesProcessor,
            JdbcBatchItemWriter<InteresCalculado> interesWriter,
            BatchSkipListener batchSkipListener) {

        return new StepBuilder(
                "procesarInteresesStep",
                jobRepository)
                .<InteresCsv, InteresCalculado>chunk(5)
                .transactionManager(transactionManager)
                .reader(interesReader)
                .processor(interesProcessor)
                .writer(interesWriter)
                .faultTolerant()
                .skip(DatoInvalidoException.class)
                .skipLimit(100)
                .skipListener(batchSkipListener)
                .build();
    }

    @Bean
    public Job interesesJob(
            JobRepository jobRepository,
            Step procesarInteresesStep) {

        return new JobBuilder(
                "interesesJob",
                jobRepository)
                .start(procesarInteresesStep)
                .build();
    }
}
