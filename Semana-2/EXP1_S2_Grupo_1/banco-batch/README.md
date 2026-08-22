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
- [9. Monitoreo y métricas](#9-monitoreo-y-métricas)
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

## 1. Descripción

Proyecto desarrollado para la asignatura **Desarrollo Backend III (PBY2203)**.

La solución moderniza procesos batch de un sistema legacy del Banco XYZ utilizando **Spring Batch**, permitiendo leer información desde archivos CSV, validar y transformar los datos, manejar registros inconsistentes, procesar información en paralelo y persistir los resultados en PostgreSQL.

Se implementaron los siguientes procesos:

1. Reporte de Transacciones Diarias.
2. Cálculo de Intereses Mensuales.
3. Generación de Estados de Cuenta Anuales.

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
- Ejecutar procesamiento paralelo con tres hilos.
- Registrar métricas de ejecución.
- Generar resúmenes diarios y anuales.
- Generar un archivo CSV anual para auditoría.

## 3. Tecnologías utilizadas

- Java 21
- Spring Boot 4.0.7
- Spring Batch 6
- Spring JDBC
- PostgreSQL
- Maven Wrapper
- Docker Desktop para ejecución local de PostgreSQL
- Visual Studio Code
- Git
- GitHub

## 4. Estructura del proyecto

```text
banco-batch/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── cl/duoc/bancobatch/
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

Persiste datos procesados en PostgreSQL o genera archivos de salida.

### JobRepository

Spring Batch utiliza PostgreSQL para almacenar metadatos relacionados con las ejecuciones de Jobs y Steps.

## 6. Procesamiento por chunks y multithreading

Los Steps principales que procesan archivos CSV utilizan:

```text
chunk size = 5
```

Para mejorar el rendimiento se configuraron pools de ejecución con un máximo de tres hilos.

```text
Transacciones    → transacciones-1..3
Intereses        → intereses-1..3
Estados anuales  → estados-anuales-1..3
```

Cada pool utiliza:

```text
corePoolSize = 3
maxPoolSize = 3
queueCapacity = 10
```

Debido a que los archivos CSV son procesados de manera concurrente, los `FlatFileItemReader` son encapsulados mediante `SynchronizedItemStreamReader`, evitando accesos simultáneos no controlados al mismo recurso de entrada.

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

El Processor valida:

- ID válido.
- Monto mayor que cero.
- Tipo de transacción permitido.
- Fecha válida.
- Normalización del tipo.
- Conversión de fecha a `LocalDate`.

Tipos permitidos:

```text
debito
credito
```

Posteriormente, `generarResumenTransaccionesStep` agrupa los datos por fecha y calcula:

- Cantidad de transacciones.
- Total de débitos.
- Total de créditos.
- Monto total procesado.

Los resultados se almacenan en:

```text
resumen_transacciones_diarias
```

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

El Processor valida:

- ID de cuenta.
- Nombre obligatorio.
- Saldo mayor que cero.
- Edad válida.
- Tipo de cuenta permitido.
- Normalización del tipo.

Tipos permitidos:

```text
ahorro
prestamo
```

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

Los cálculos monetarios utilizan `BigDecimal`.

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

- Cuenta válida.
- Fecha válida.
- Monto distinto de cero.
- Tipo de movimiento permitido.
- Normalización del tipo.
- Normalización de descripción.

Tipos admitidos:

```text
deposito
retiro
compra
```

Reglas de monto:

```text
deposito → positivo
retiro   → negativo
compra   → negativo
```

El segundo Step agrupa movimientos por cuenta y calcula:

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

## 8. Manejo de errores y tolerancia a fallos

La aplicación utiliza:

```text
DatoInvalidoException
```

para representar registros de entrada que incumplen las reglas de negocio.

### Política de Skip

Los Steps están configurados como `faultTolerant`.

Los datos inválidos se omiten mediante:

```text
skipLimit = 100
```

El `BatchSkipListener` registra:

- Etapa donde ocurrió el error.
- Registro descartado.
- Motivo del error.

Ejemplo:

```text
REGISTRO OMITIDO |
etapa=PROCESS |
registro=... |
motivo=...
```

### Política de Retry

Los errores transitorios de acceso a datos utilizan:

```text
TransientDataAccessException
retryLimit = 3
```

La estrategia aplicada es:

```text
Dato inválido       → SKIP
Error temporal BD   → RETRY
```

Un registro inválido no se reintenta porque seguiría incumpliendo la misma regla, mientras que un error temporal de base de datos puede resolverse en un intento posterior.

## 9. Monitoreo y métricas

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
leidos=10 |
escritos=8 |
omitidosProceso=2 |
commits=2 |
rollbacks=0 |
duracionMs=88
```

Resultados observados durante las pruebas:

### Transacciones

```text
leidos=10
escritos=8
omitidosProceso=2
commits=2
rollbacks=0
status=COMPLETED
```

### Intereses

```text
leidos=8
escritos=6
omitidosProceso=2
commits=2
rollbacks=0
status=COMPLETED
```

### Estados anuales

```text
procesarMovimientosAnualesStep
leidos=9
escritos=8
omitidosProceso=1
commits=2
rollbacks=0

generarResumenAnualStep
leidos=7
escritos=7
omitidosProceso=0
commits=2
rollbacks=0

status=COMPLETED
```

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

banco.interes.ahorro=0.01
banco.interes.prestamo=0.02
```

El Job a ejecutar puede seleccionarse mediante:

```properties
spring.batch.job.name=transaccionesJob
```

Valores posibles:

```text
transaccionesJob
interesesJob
estadosAnualesJob
```

La contraseña de PostgreSQL no se almacena directamente en el repositorio.

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

## 13. Configurar PostgreSQL

La aplicación espera:

```text
Host: localhost
Puerto: 5432
Base: banco_batch
Usuario: postgres
```

La contraseña se entrega mediante:

```text
DB_PASSWORD
```

En PowerShell:

```powershell
$env:DB_PASSWORD="postgres"
```

Si PostgreSQL se ejecuta en Docker, debe existir un contenedor disponible en:

```text
localhost:5432
```

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

## 15. Ejecutar los Jobs

El Job seleccionado se define en:

```text
src/main/resources/application.properties
```

Ejemplo:

```properties
spring.batch.job.name=transaccionesJob
```

Luego se ejecuta:

```powershell
$fecha = Get-Date -Format "yyyyMMddHHmmss"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=fechaEjecucion=$fecha"
```

Para ejecutar otro Job, se cambia el valor de:

```properties
spring.batch.job.name
```

por:

```text
interesesJob
```

o:

```text
estadosAnualesJob
```

## 16. Re-ejecución de procesos

Spring Batch identifica una instancia de Job según sus parámetros.

Si se intenta ejecutar nuevamente un Job completado con exactamente los mismos parámetros, Spring Batch evita repetir la misma instancia.

Por este motivo se utiliza:

```text
fechaEjecucion
```

como parámetro identificador.

Ejemplo:

```powershell
$fecha = Get-Date -Format "yyyyMMddHHmmss"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=fechaEjecucion=$fecha"
```

Además, los Writers utilizan operaciones como:

```sql
ON CONFLICT
DO UPDATE
```

o:

```sql
ON CONFLICT
DO NOTHING
```

para evitar duplicar información en las tablas de negocio.

## 17. Resultados generados

### Transacciones

```text
transacciones_procesadas
resumen_transacciones_diarias
```

### Intereses

```text
intereses_calculados
```

### Estados anuales

```text
movimientos_anuales
estados_cuenta_anuales
output/estados_cuenta_anuales.csv
```

Ejemplo del archivo anual generado:

```text
cuenta_id,total_depositos,total_retiros,total_compras,saldo_neto,cantidad_movimientos
101,1000.00,-500.00,0,500.00,2
102,1500.00,0,0,1500.00,1
103,2000.00,0,0,2000.00,1
104,0,0,-100.00,-100.00,1
105,2500.00,0,0,2500.00,1
106,3000.00,0,0,3000.00,1
108,2000.00,0,0,2000.00,1
```

## 18. Consultas de verificación

### Transacciones procesadas

```sql
SELECT *
FROM transacciones_procesadas
ORDER BY id;
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

## 19. Propuesta técnica

Las principales decisiones técnicas fueron:

- Java 21.
- Spring Boot 4.0.7.
- Spring Batch 6.
- PostgreSQL como base de datos relacional.
- JDBC para persistencia.
- `BigDecimal` para cálculos monetarios.
- Modelos separados para CSV y datos procesados.
- Validación mediante `ItemProcessor`.
- Procesamiento por chunks de tamaño 5.
- Pools de tres hilos.
- `SynchronizedItemStreamReader` para lectura concurrente segura.
- `Skip` para registros inválidos.
- `Retry` para errores transitorios de acceso a datos.
- `BatchSkipListener` para registrar errores.
- `BatchStepListener` para registrar métricas.
- Uso de parámetros de ejecución para permitir nuevas instancias de Job.
- `ON CONFLICT` para evitar duplicados.
- Uso de variables de entorno para credenciales.
- Generación de resultados en PostgreSQL y CSV.

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

## 21. Evidencias

Las evidencias de ejecución se almacenan en:

```text
evidencias/
```

Incluyen:

- Ejecución de `transaccionesJob`.
- Ejecución de `interesesJob`.
- Ejecución de `estadosAnualesJob`.
- Estados `COMPLETED`.
- Registros omitidos.
- Resultados persistidos en PostgreSQL.
- Archivo anual generado.
- Conteos de lectura, escritura y skips.

Se recomienda complementar las evidencias con capturas donde se observen:

```text
transacciones-*
intereses-*
estados-anuales-*
```

y los logs:

```text
STEP INICIADO
STEP FINALIZADO
```

## 22. Resultado final

La solución implementa los tres procesos requeridos para modernizar los procesos legacy del Banco XYZ:

```text
Reporte de Transacciones Diarias
Cálculo de Intereses Mensuales
Generación de Estados de Cuenta Anuales
```

Los Jobs procesan información desde archivos CSV, aplican validaciones y transformaciones mediante `ItemProcessor`, persisten datos en PostgreSQL, manejan registros inválidos mediante Skip, implementan Retry para errores transitorios, procesan información con chunks de tamaño 5 y tres hilos de ejecución, y registran métricas mediante listeners.

Las tres ejecuciones fueron verificadas con estado:

```text
COMPLETED
```
