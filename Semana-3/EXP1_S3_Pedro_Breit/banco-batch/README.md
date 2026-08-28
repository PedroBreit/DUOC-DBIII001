# Migración de Procesos Legacy Banco XYZ con Spring Batch

## Índice

- [1. Descripción](#1-descripción)
- [2. Objetivo](#2-objetivo)
- [3. Tecnologías utilizadas](#3-tecnologías-utilizadas)
- [4. Estructura del proyecto](#4-estructura-del-proyecto)
- [5. Arquitectura Batch](#5-arquitectura-batch)
- [6. Procesamiento por chunks y multithreading](#6-procesamiento-por-chunks-y-multithreading)
- [7. Procesos implementados](#7-procesos-implementados)
- [8. Manejo de errores y tolerancia a fallos](#8-manejo-de-errores-y-tolerancia-a-fallos)
- [9. Monitoreo, métricas y rendimiento](#9-monitoreo-métricas-y-rendimiento)
- [10. Base de datos](#10-base-de-datos)
- [11. Configuración](#11-configuración)
- [12. Requisitos para ejecutar](#12-requisitos-para-ejecutar)
- [13. Configurar PostgreSQL](#13-configurar-postgresql)
- [14. Compilar el proyecto](#14-compilar-el-proyecto)
- [15. Ejecutar los Jobs](#15-ejecutar-los-jobs)
- [16. Re-ejecución de procesos](#16-re-ejecución-de-procesos)
- [17. Resultados generados](#17-resultados-generados)
- [18. Consultas de verificación](#18-consultas-de-verificación)
- [19. Propuesta técnica](#19-propuesta-técnica)
- [20. Supuestos técnicos](#20-supuestos-técnicos)
- [21. Evidencias](#21-evidencias)
- [22. Resultado final](#22-resultado-final)

---

## 1. Descripción

Proyecto desarrollado para la asignatura **Desarrollo Backend III (PBY2203)**.

La solución moderniza procesos batch de un sistema legacy del Banco XYZ utilizando **Spring Batch**, permitiendo leer información desde archivos CSV, validar y transformar datos, manejar registros inconsistentes, aplicar tolerancia a fallos, ejecutar procesamiento multihilo y persistir los resultados en PostgreSQL.

Se implementaron los siguientes procesos:

1. Reporte de Transacciones Diarias.
2. Cálculo de Intereses Mensuales.
3. Generación de Estados de Cuenta Anuales.

---

## 2. Objetivo

Modernizar procesos batch de un sistema bancario legacy mediante una arquitectura basada en Spring Batch.

La solución implementa el flujo:

```text
Entrada CSV
    ↓
ItemReader
    ↓
ItemProcessor
    ↓
ItemWriter
    ↓
PostgreSQL / Archivo CSV
```

Los procesos permiten:

- Leer archivos CSV provenientes del sistema legacy.
- Validar y normalizar información.
- Detectar registros inconsistentes.
- Omitir registros inválidos sin detener el Job completo.
- Reintentar errores transitorios de acceso a datos.
- Persistir resultados en PostgreSQL.
- Procesar información mediante chunks.
- Ejecutar procesamiento multihilo con una cantidad de threads configurable.
- Comparar configuraciones de 1, 3 y 5 threads.
- Registrar métricas de ejecución.
- Generar resúmenes diarios y anuales.
- Generar un archivo CSV anual para auditoría.

---

## 3. Tecnologías utilizadas

- Java 21
- Spring Boot 4.0.7
- Spring Batch 6
- Spring JDBC
- PostgreSQL
- Maven Wrapper
- Docker Desktop
- Visual Studio Code
- Git
- GitHub

---

## 4. Estructura del proyecto

```text
banco-batch/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── cl/duoc/bancobatch/
│   │   │       │
│   │   │       ├── config/
│   │   │       │   ├── TransaccionesJobConfig.java
│   │   │       │   ├── InteresesJobConfig.java
│   │   │       │   └── EstadosAnualesJobConfig.java
│   │   │       │
│   │   │       ├── exception/
│   │   │       │   └── DatoInvalidoException.java
│   │   │       │
│   │   │       ├── listener/
│   │   │       │   ├── BatchSkipListener.java
│   │   │       │   └── BatchStepListener.java
│   │   │       │
│   │   │       ├── policy/
│   │   │       │   └── BancoSkipPolicy.java
│   │   │       │
│   │   │       ├── model/
│   │   │       │   ├── TransaccionCsv.java
│   │   │       │   ├── Transaccion.java
│   │   │       │   ├── ResumenTransaccionDiaria.java
│   │   │       │   ├── InteresCsv.java
│   │   │       │   ├── InteresCalculado.java
│   │   │       │   ├── MovimientoAnualCsv.java
│   │   │       │   ├── MovimientoAnual.java
│   │   │       │   └── EstadoCuentaAnual.java
│   │   │       │
│   │   │       ├── processor/
│   │   │       │   ├── TransaccionProcessor.java
│   │   │       │   ├── InteresProcessor.java
│   │   │       │   └── MovimientoAnualProcessor.java
│   │   │       │
│   │   │       └── BancoBatchApplication.java
│   │   │
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── schema.sql
│   │       └── input/
│   │           ├── transacciones.csv
│   │           ├── intereses.csv
│   │           └── cuentas_anuales.csv
│   │
│   └── test/
│
├── output/
│   └── estados_cuenta_anuales.csv
│
├── evidencias/
│
├── pom.xml
├── mvnw
├── mvnw.cmd
└── README.md
```

---

## 5. Arquitectura Batch

La solución utiliza los principales componentes de Spring Batch.

### Job

Representa un proceso batch completo.

El proyecto contiene tres Jobs:

```text
transaccionesJob
interesesJob
estadosAnualesJob
```

### Step

Cada Job está compuesto por uno o más Steps responsables de una etapa específica del procesamiento.

### ItemReader

Lee datos desde archivos CSV o desde PostgreSQL.

### ItemProcessor

Valida, transforma y normaliza los datos antes de escribirlos.

### ItemWriter

Persiste los datos procesados en PostgreSQL o genera archivos de salida.

### JobRepository

Spring Batch utiliza PostgreSQL para almacenar metadatos relacionados con las ejecuciones de Jobs y Steps.

---

## 6. Procesamiento por chunks y multithreading

Los Steps principales que procesan archivos CSV utilizan procesamiento por chunks.

La configuración por defecto es:

```properties
banco.batch.chunk-size=5
banco.batch.threads=3
banco.batch.queue-capacity=10
```

La cantidad de threads no está definida de forma fija en el código. Puede modificarse mediante:

```properties
banco.batch.threads
```

Cada Job utiliza un `ThreadPoolTaskExecutor` configurable.

```text
corePoolSize = threads
maxPoolSize = threads
queueCapacity = 10
```

Los pools utilizan los siguientes prefijos:

```text
Transacciones    → transacciones-
Intereses        → intereses-
Estados anuales  → estados-anuales-
```

Debido a que los archivos CSV son procesados de manera concurrente, los `FlatFileItemReader` son encapsulados mediante `SynchronizedItemStreamReader`, evitando accesos simultáneos no controlados al mismo archivo de entrada.

Para evaluar el escalado se realizaron pruebas utilizando:

```text
1 thread
3 threads
5 threads
```

Los resultados demostraron que aumentar la cantidad de threads no necesariamente reduce el tiempo total de ejecución.

La configuración más conveniente depende de la naturaleza y carga de cada Job.

---

## 7. Procesos implementados

### 7.1 Reporte de Transacciones Diarias

Job:

```text
transaccionesJob
```

Está compuesto por:

```text
transaccionesJob
│
├── procesarTransaccionesStep
└── generarResumenTransaccionesStep
```

El primer Step lee:

```text
src/main/resources/input/transacciones.csv
```

Flujo:

```text
transacciones.csv
        ↓
FlatFileItemReader
        ↓
SynchronizedItemStreamReader
        ↓
TransaccionProcessor
        ↓
JdbcBatchItemWriter
        ↓
transacciones_procesadas
```

El `TransaccionProcessor` aplica las siguientes reglas:

- ID obligatorio y mayor que cero.
- Monto obligatorio.
- Monto distinto de cero.
- Normalización de montos negativos mediante valor absoluto.
- Tipo de transacción permitido.
- Normalización del tipo.
- Eliminación de acentos.
- Conversión del tipo a minúsculas.
- Validación y conversión de múltiples formatos de fecha.

Tipos permitidos:

```text
debito
credito
```

Formatos de fecha admitidos:

```text
yyyy-MM-dd
yyyy/MM/dd
dd-MM-yyyy
dd/MM/yyyy
```

Los registros inválidos son omitidos mediante `BancoSkipPolicy`.

Posteriormente, `generarResumenTransaccionesStep` agrupa los datos por fecha y calcula:

- Cantidad de transacciones.
- Total de débitos.
- Total de créditos.
- Monto total procesado.

Los resultados se almacenan en:

```text
resumen_transacciones_diarias
```

---

### 7.2 Cálculo de Intereses Mensuales

Job:

```text
interesesJob
```

Step:

```text
procesarInteresesStep
```

Flujo:

```text
intereses.csv
      ↓
FlatFileItemReader
      ↓
SynchronizedItemStreamReader
      ↓
InteresProcessor
      ↓
JdbcBatchItemWriter
      ↓
intereses_calculados
```

El `InteresProcessor` valida:

- ID de cuenta válido.
- Nombre obligatorio.
- Rechazo del nombre `Unknown`.
- Saldo obligatorio.
- Saldo mayor que cero.
- Edad obligatoria.
- Edad entre 0 y 120 años.
- Tipo de cuenta permitido.
- Normalización del tipo.
- Eliminación de acentos.

Tipos permitidos:

```text
ahorro
prestamo
```

Como regla de normalización, el valor:

```text
hipoteca
```

se transforma a:

```text
prestamo
```

Otros tipos desconocidos se consideran inválidos y son omitidos mediante la política de Skip.

Las tasas se configuran en:

```properties
banco.interes.ahorro=0.01
banco.interes.prestamo=0.02
```

El cálculo utilizado es:

```text
interes = saldoInicial × tasa

saldoFinal = saldoInicial + interes
```

Los cálculos monetarios utilizan `BigDecimal` y redondeo a dos decimales mediante `RoundingMode.HALF_UP`.

---

### 7.3 Generación de Estados de Cuenta Anuales

Job:

```text
estadosAnualesJob
```

Está compuesto por:

```text
estadosAnualesJob
│
├── procesarMovimientosAnualesStep
└── generarResumenAnualStep
```

El primer Step lee:

```text
src/main/resources/input/cuentas_anuales.csv
```

Flujo:

```text
cuentas_anuales.csv
        ↓
FlatFileItemReader
        ↓
SynchronizedItemStreamReader
        ↓
MovimientoAnualProcessor
        ↓
JdbcBatchItemWriter
        ↓
movimientos_anuales
```

Las reglas incluyen:

- Cuenta obligatoria y válida.
- Fecha obligatoria y válida.
- Soporte de múltiples formatos de fecha.
- Monto obligatorio.
- Monto distinto de cero.
- Tipo de movimiento permitido.
- Normalización del tipo.
- Eliminación de acentos.
- Normalización de descripción.

Tipos admitidos:

```text
deposito
retiro
compra
```

Otros tipos, por ejemplo `pago`, son considerados inválidos.

Reglas de normalización del monto:

```text
deposito → positivo
retiro   → negativo
compra   → negativo
```

Formatos de fecha admitidos:

```text
yyyy-MM-dd
yyyy/MM/dd
dd-MM-yyyy
dd/MM/yyyy
```

Si la descripción se encuentra vacía se normaliza como:

```text
SIN DESCRIPCION
```

El segundo Step agrupa los movimientos por cuenta y calcula:

- Total de depósitos.
- Total de retiros.
- Total de compras.
- Saldo neto.
- Cantidad de movimientos.

Los resultados se almacenan en:

```text
estados_cuenta_anuales
```

Además, un `CompositeItemWriter` genera:

```text
output/estados_cuenta_anuales.csv
```

---

## 8. Manejo de errores y tolerancia a fallos

La aplicación utiliza:

```text
DatoInvalidoException
```

para representar registros de entrada que incumplen las reglas de negocio.

### Política de Skip

Se implementó una política personalizada:

```text
BancoSkipPolicy
```

que implementa `SkipPolicy`.

La política permite omitir:

```text
DatoInvalidoException
FlatFileParseException
```

El límite se configura mediante:

```properties
banco.batch.skip-limit=1000
```

Si se alcanza el límite configurado se lanza:

```text
SkipLimitExceededException
```

Además, `BatchSkipListener` registra:

- Etapa donde ocurrió el error.
- Registro descartado.
- Tipo de excepción.
- Motivo del rechazo.

Ejemplo:

```text
SKIP POLICY |
registro omitido=... |
excepcion=... |
motivo=...
```

También se registra:

```text
REGISTRO OMITIDO |
etapa=PROCESS |
registro=... |
motivo=...
```

De esta manera, un registro inválido no detiene completamente el Job.

### Política de Retry

Para errores transitorios de acceso a PostgreSQL se utiliza una política de reintentos basada en:

```text
TransientDataAccessException
```

Configuración:

```properties
banco.batch.retry-limit=3
banco.batch.backoff-ms=500
```

La política utiliza:

```text
maxRetries = 3
delay = 500 ms
```

La estrategia aplicada es:

```text
Dato inválido       → SKIP
Error temporal BD   → RETRY
```

Los registros que incumplen reglas de negocio no son reintentados, ya que seguirían incumpliendo la misma validación.

En cambio, un fallo temporal de acceso a base de datos puede resolverse en un intento posterior.

---

## 9. Monitoreo, métricas y rendimiento

Se implementó:

```text
BatchStepListener
```

para registrar métricas antes y después de cada Step.

Se registran:

- Estado del Step.
- Cantidad de registros leídos.
- Cantidad de registros escritos.
- Omisiones en lectura.
- Omisiones en procesamiento.
- Omisiones en escritura.
- Cantidad de commits.
- Cantidad de rollbacks.
- Duración del Step.

Ejemplo:

```text
STEP FINALIZADO |
step=procesarTransaccionesStep |
status=COMPLETED |
leidos=1000 |
escritos=482 |
omitidosProceso=518 |
commits=200 |
rollbacks=0 |
duracionMs=...
```

### Resultados funcionales

Las pruebas finales se realizaron utilizando los archivos oficiales de Semana 3 con 1000 registros de entrada por proceso.

### Transacciones

```text
procesarTransaccionesStep

leidos=1000
escritos=482
omitidosLectura=0
omitidosProceso=518
omitidosEscritura=0
commits=200
rollbacks=0
status=COMPLETED
```

El Step de resumen obtuvo:

```text
generarResumenTransaccionesStep

leidos=265
escritos=265
omitidosProceso=0
rollbacks=0
status=COMPLETED
```

### Intereses

```text
procesarInteresesStep

leidos=1000
escritos=370
omitidosLectura=0
omitidosProceso=630
omitidosEscritura=0
commits=200
rollbacks=0
status=COMPLETED
```

Los datos procesados corresponden a cuentas identificadas mediante `cuenta_id`.

En PostgreSQL se mantienen 50 cuentas actualizadas en:

```text
intereses_calculados
```

### Estados anuales

```text
procesarMovimientosAnualesStep

leidos=1000
escritos=898
omitidosLectura=0
omitidosProceso=102
omitidosEscritura=0
commits=200
rollbacks=0
status=COMPLETED
```

El resumen anual obtuvo:

```text
generarResumenAnualStep

leidos=20
escritos=20
omitidosProceso=0
commits=4
rollbacks=0
status=COMPLETED
```

### Comparación de rendimiento

Las pruebas se realizaron manteniendo:

```text
chunk-size = 5
```

y variando la cantidad de threads.

| Job | 1 thread | 3 threads | 5 threads | Mejor configuración observada |
|---|---:|---:|---:|---|
| transaccionesJob | 1514 ms | 1365 ms | 1384 ms | 3 threads |
| interesesJob | 1112 ms | 1135 ms | 1170 ms | 1 thread |
| estadosAnualesJob | 1318 ms | 1274 ms | 1146 ms | 5 threads |

Los resultados muestran que aumentar la cantidad de threads no garantiza automáticamente una mejora de rendimiento.

Para `transaccionesJob`, la mejor ejecución observada se obtuvo con 3 threads.

Para `interesesJob`, el mejor tiempo se obtuvo con 1 thread, debido a que el costo adicional de administrar múltiples threads no produjo una mejora para este tipo de procesamiento.

Para `estadosAnualesJob`, la mejor ejecución observada se obtuvo con 5 threads.

Por lo tanto, la configuración óptima depende del tipo de carga y procesamiento realizado por cada Job.

---

## 10. Base de datos

La solución utiliza PostgreSQL.

Base requerida:

```text
banco_batch
```

Spring Batch utiliza tablas internas para mantener:

- Instancias de Job.
- Ejecuciones.
- Parámetros.
- Estados.
- Steps.
- Conteos de lectura y escritura.
- Registros omitidos.

Las tablas de negocio son:

```text
transacciones_procesadas
resumen_transacciones_diarias
intereses_calculados
movimientos_anuales
estados_cuenta_anuales
```

Estas tablas se crean mediante:

```text
src/main/resources/schema.sql
```

---

## 11. Configuración

Archivo:

```text
src/main/resources/application.properties
```

Configuración principal:

```properties
spring.application.name=banco-batch

spring.datasource.url=jdbc:postgresql://localhost:5432/banco_batch
spring.datasource.username=postgres
spring.datasource.password=${DB_PASSWORD}

spring.batch.jdbc.initialize-schema=always
spring.batch.job.enabled=true

spring.sql.init.mode=always

logging.level.org.springframework.batch=INFO

# Reglas de negocio - Intereses
banco.interes.ahorro=0.01
banco.interes.prestamo=0.02

# Semana 3 - Optimizacion Batch
banco.batch.chunk-size=5
banco.batch.threads=3
banco.batch.queue-capacity=10
banco.batch.retry-limit=3
banco.batch.skip-limit=1000
banco.batch.backoff-ms=500
```

Las propiedades permiten modificar el comportamiento del procesamiento sin necesidad de recompilar el proyecto.

La contraseña de PostgreSQL no se almacena directamente en el repositorio.

---

## 12. Requisitos para ejecutar

Se requiere:

```text
Java 21
PostgreSQL
Git
```

Opcionalmente, PostgreSQL puede ejecutarse mediante Docker Desktop.

El proyecto incluye Maven Wrapper, por lo que Maven no necesita instalarse de forma independiente.

Verificar Java:

```powershell
java -version
```

Verificar Maven Wrapper:

```powershell
.\mvnw.cmd -version
```

---

## 13. Configurar PostgreSQL

La aplicación espera:

```text
Host: localhost
Puerto: 5432
Base: banco_batch
Usuario: postgres
```

La contraseña se entrega mediante la variable de entorno:

```text
DB_PASSWORD
```

En PowerShell:

```powershell
$env:DB_PASSWORD="postgres"
```

Si PostgreSQL se ejecuta mediante Docker, se puede utilizar un contenedor disponible en:

```text
localhost:5432
```

Ejemplo de verificación:

```powershell
docker ps
```

---

## 14. Compilar el proyecto

Desde la carpeta:

```text
banco-batch
```

ejecutar:

```powershell
.\mvnw.cmd clean compile
```

Resultado esperado:

```text
BUILD SUCCESS
```

---

## 15. Ejecutar los Jobs

Antes de ejecutar se debe configurar la contraseña de PostgreSQL:

```powershell
$env:DB_PASSWORD="postgres"
```

Cada ejecución utiliza un parámetro `run.id` único:

```powershell
$runId = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
```

### Transacciones

Configuración seleccionada durante las pruebas:

```text
threads = 3
chunk-size = 5
```

Ejecución:

```powershell
$runId = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()

.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=transaccionesJob --banco.batch.threads=3 --banco.batch.chunk-size=5 run.id=$runId"
```

### Intereses

Configuración seleccionada durante las pruebas:

```text
threads = 1
chunk-size = 5
```

Ejecución:

```powershell
$runId = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()

.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=interesesJob --banco.batch.threads=1 --banco.batch.chunk-size=5 run.id=$runId"
```

### Estados anuales

Configuración seleccionada durante las pruebas:

```text
threads = 5
chunk-size = 5
```

Ejecución:

```powershell
$runId = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()

.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=estadosAnualesJob --banco.batch.threads=5 --banco.batch.chunk-size=5 run.id=$runId"
```

La cantidad de threads también puede modificarse manualmente mediante:

```text
--banco.batch.threads=N
```

y el tamaño del chunk mediante:

```text
--banco.batch.chunk-size=N
```

sin necesidad de recompilar la aplicación.

---

## 16. Re-ejecución de procesos

Spring Batch identifica una instancia de Job según sus parámetros.

Si se intenta ejecutar nuevamente un Job completado utilizando exactamente los mismos parámetros, Spring Batch evita repetir la misma instancia.

Por este motivo se utiliza:

```text
run.id
```

como parámetro identificador único.

Ejemplo:

```powershell
$runId = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()

.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=transaccionesJob run.id=$runId"
```

Además, los Writers utilizan operaciones como:

```sql
ON CONFLICT DO UPDATE
```

o:

```sql
ON CONFLICT DO NOTHING
```

según el tipo de información procesada, evitando inconsistencias o duplicaciones no deseadas en las tablas de negocio.

---

## 17. Resultados generados

### Transacciones

```text
transacciones_procesadas
resumen_transacciones_diarias
```

Durante las pruebas finales:

```text
transacciones_procesadas = 482 registros
resumen_transacciones_diarias = 265 registros
```

### Intereses

```text
intereses_calculados
```

Durante las pruebas finales:

```text
cuentas actualizadas = 50
```

Cada registro contiene:

```text
cuenta_id
nombre
saldo_inicial
edad
tipo
tasa
interes
saldo_final
```

### Estados anuales

```text
movimientos_anuales
estados_cuenta_anuales
output/estados_cuenta_anuales.csv
```

Durante las pruebas finales:

```text
movimientos_anuales = 898 registros
estados_cuenta_anuales = 20 registros
```

El archivo anual contiene:

```text
cuenta_id
total_depositos
total_retiros
total_compras
saldo_neto
cantidad_movimientos
```

---

## 18. Consultas de verificación

### Transacciones procesadas

```sql
SELECT *
FROM transacciones_procesadas
ORDER BY id;
```

### Cantidad de transacciones procesadas

```sql
SELECT COUNT(*) AS total_transacciones
FROM transacciones_procesadas;
```

### Resumen diario

```sql
SELECT *
FROM resumen_transacciones_diarias
ORDER BY fecha;
```

### Intereses

```sql
SELECT *
FROM intereses_calculados
ORDER BY cuenta_id;
```

### Cantidad de cuentas con intereses

```sql
SELECT COUNT(*) AS cuentas_actualizadas
FROM intereses_calculados;
```

### Movimientos anuales

```sql
SELECT *
FROM movimientos_anuales
ORDER BY cuenta_id, fecha;
```

### Estados anuales

```sql
SELECT *
FROM estados_cuenta_anuales
ORDER BY cuenta_id;
```

### Conteo de resultados anuales

```sql
SELECT COUNT(*) AS total_movimientos
FROM movimientos_anuales;

SELECT COUNT(*) AS total_estados
FROM estados_cuenta_anuales;
```

### Ejecución de Steps

```sql
SELECT
    step_name,
    status,
    read_count,
    write_count,
    process_skip_count
FROM batch_step_execution
ORDER BY step_execution_id;
```

---

## 19. Propuesta técnica

Las principales decisiones técnicas fueron:

- Java 21.
- Spring Boot 4.0.7.
- Spring Batch 6.
- PostgreSQL como base de datos relacional.
- JDBC para persistencia.
- `BigDecimal` para cálculos monetarios.
- Modelos separados para datos CSV y datos procesados.
- Validación y transformación mediante `ItemProcessor`.
- Procesamiento mediante chunks configurables.
- `ThreadPoolTaskExecutor` con cantidad de threads parametrizable.
- Comparación de configuraciones de 1, 3 y 5 threads.
- `SynchronizedItemStreamReader` para lectura concurrente segura.
- `BancoSkipPolicy` personalizada para registros inválidos.
- `RetryPolicy` con 3 reintentos para errores transitorios.
- Backoff configurable de 500 ms.
- `BatchSkipListener` para registrar registros omitidos.
- `BatchStepListener` para registrar métricas de ejecución.
- Parámetros de chunk, threads, queue, retry, skip y backoff configurables.
- Uso de parámetros `run.id` para permitir nuevas instancias de Job.
- Uso de `ON CONFLICT` para controlar actualizaciones y duplicados.
- Variables de entorno para credenciales.
- Generación de resultados tanto en PostgreSQL como en archivo CSV.

---

## 20. Supuestos técnicos

Las instrucciones no proporcionan tasas específicas para el cálculo de intereses.

Por este motivo se definieron:

```text
Ahorro   = 1%
Préstamo = 2%
```

Estos valores pueden modificarse mediante:

```properties
banco.interes.ahorro
banco.interes.prestamo
```

También se considera que:

```text
deposito → monto positivo
retiro   → monto negativo
compra   → monto negativo
```

Los movimientos con monto cero son considerados inconsistentes.

Los montos negativos de las transacciones son normalizados utilizando su valor absoluto cuando corresponde.

El valor:

```text
hipoteca
```

es normalizado como:

```text
prestamo
```

para el proceso de cálculo de intereses.

---

## 21. Evidencias

Las evidencias de ejecución incluyen:

- Ejecución de `transaccionesJob`.
- Resultados de `transaccionesJob` almacenados en PostgreSQL.
- Ejecución de `interesesJob`.
- Resultados del cálculo de intereses almacenados en PostgreSQL.
- Ejecución de `estadosAnualesJob`.
- Resultados de estados anuales almacenados en PostgreSQL.
- Registros inválidos omitidos mediante `BancoSkipPolicy`.
- Continuidad de los Jobs hasta estado `COMPLETED`.
- Comparación de rendimiento con 1, 3 y 5 threads.
- Configuración parametrizable de Spring Batch.
- Implementación de `BancoSkipPolicy`.
- Implementación de `RetryPolicy`.
- Implementación de `ThreadPoolTaskExecutor`.
- Validaciones realizadas mediante `TransaccionProcessor`.
- Validaciones y cálculo realizados mediante `InteresProcessor`.
- Validaciones y normalización realizadas mediante `MovimientoAnualProcessor`.
- Compilación final del proyecto con `BUILD SUCCESS`.

Las evidencias permiten comprobar tanto el funcionamiento de los Jobs como las estrategias de tolerancia a fallos y optimización aplicadas durante la Semana 3.

---

## 22. Resultado final

La solución implementa los tres procesos requeridos para modernizar los procesos legacy del Banco XYZ:

```text
Reporte de Transacciones Diarias

Cálculo de Intereses Mensuales

Generación de Estados de Cuenta Anuales
```

Los Jobs procesan información desde archivos CSV, aplican validaciones y transformaciones mediante `ItemProcessor`, persisten resultados en PostgreSQL, manejan registros inválidos mediante una `BancoSkipPolicy` personalizada e implementan una `RetryPolicy` para errores transitorios.

El procesamiento utiliza chunks configurables y `ThreadPoolTaskExecutor`, permitiendo modificar la cantidad de threads sin alterar el código fuente.

Se realizaron pruebas con configuraciones de 1, 3 y 5 threads, determinándose que la mejor configuración observada para cada Job fue:

```text
transaccionesJob   → 3 threads
interesesJob       → 1 thread
estadosAnualesJob  → 5 threads
```

Los resultados funcionales finales fueron:

```text
transaccionesJob
1000 leídos
482 escritos
518 omitidos
0 rollbacks

interesesJob
1000 leídos
370 escritos
630 omitidos
0 rollbacks

estadosAnualesJob
1000 leídos
898 escritos
102 omitidos
0 rollbacks
```

Las tres ejecuciones fueron verificadas con estado:

```text
COMPLETED
```

Finalmente, el proyecto fue compilado correctamente mediante Maven obteniendo:

```text
BUILD SUCCESS
```