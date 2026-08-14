# Migración de Procesos Legacy Banco XYZ con Spring Batch

## Índice

* [1. Descripción](#1-descripción)
* [2. Objetivo](#2-objetivo)
* [3. Tecnologías utilizadas](#3-tecnologías-utilizadas)
* [4. Estructura del proyecto](#4-estructura-del-proyecto)
* [5. Arquitectura Batch](#5-arquitectura-batch)
* [6. Procesos implementados](#6-procesos-implementados)

  * [6.1 Reporte de Transacciones Diarias](#61-reporte-de-transacciones-diarias)
  * [6.2 Cálculo de Intereses Mensuales](#62-cálculo-de-intereses-mensuales)
  * [6.3 Generación de Estados de Cuenta Anuales](#63-generación-de-estados-de-cuenta-anuales)
* [7. Manejo de errores](#7-manejo-de-errores)
* [8. Base de datos](#8-base-de-datos)
* [9. Configuración](#9-configuración)
* [10. Requisitos para ejecutar](#10-requisitos-para-ejecutar)
* [11. Configurar contraseña PostgreSQL](#11-configurar-contraseña-postgresql)
* [12. Compilar el proyecto](#12-compilar-el-proyecto)
* [13. Generar el archivo JAR](#13-generar-el-archivo-jar)
* [14. Ejecutar los Jobs](#14-ejecutar-los-jobs)
* [15. Re-ejecución de procesos](#15-re-ejecución-de-procesos)
* [16. Resultados generados](#16-resultados-generados)
* [17. Consultas de verificación](#17-consultas-de-verificación)
* [18. Verificación de ejecución de Steps](#18-verificación-de-ejecución-de-steps)
* [19. Propuesta técnica](#19-propuesta-técnica)
* [20. Supuestos técnicos](#20-supuestos-técnicos)
* [21. Evidencias](#21-evidencias)
* [22. Resultado final](#22-resultado-final)

## 1. Descripción

Proyecto desarrollado para la asignatura **Desarrollo Backend III (PBY2203)**.

La solución implementa una migración de procesos batch pertenecientes a un sistema legacy del Banco XYZ utilizando **Spring Batch**, permitiendo leer información desde archivos CSV, validar y transformar los datos, manejar registros inconsistentes y persistir los resultados en una base de datos PostgreSQL.

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

Los procesos están diseñados para:

* Leer archivos CSV provenientes del sistema legacy.
* Validar datos antes de procesarlos.
* Normalizar información.
* Detectar registros inconsistentes.
* Omitir registros inválidos sin detener completamente el Job.
* Persistir resultados en PostgreSQL.
* Registrar la ejecución de Jobs y Steps.
* Generar resúmenes de información procesada.
* Generar un archivo CSV de estados de cuenta para auditoría.

---

## 3. Tecnologías utilizadas

* Java 21
* Spring Boot 4.0.7
* Spring Batch
* Spring JDBC
* PostgreSQL 18
* Maven
* Visual Studio Code
* Git
* GitHub

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
│   │   │       │   └── BatchSkipListener.java
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
│   │       │
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
├── .gitignore
├── pom.xml
├── mvnw
├── mvnw.cmd
└── README.md
```

---

## 5. Arquitectura Batch

La solución utiliza los principales componentes de Spring Batch:

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

Lee los datos desde los archivos CSV o desde PostgreSQL.

### ItemProcessor

Valida, transforma y normaliza los datos antes de escribirlos.

### ItemWriter

Persiste los datos procesados en PostgreSQL o genera archivos de salida.

### JobRepository

Spring Batch utiliza PostgreSQL para almacenar los metadatos correspondientes a las ejecuciones de Jobs y Steps.

---

# 6. Procesos implementados

## 6.1 Reporte de Transacciones Diarias

Job:

```text
transaccionesJob
```

Está compuesto por dos Steps:

```text
transaccionesJob
│
├── procesarTransaccionesStep
│
└── generarResumenTransaccionesStep
```

### Step 1: procesarTransaccionesStep

Lee:

```text
src/main/resources/input/transacciones.csv
```

El flujo es:

```text
transacciones.csv
        ↓
FlatFileItemReader
        ↓
TransaccionCsv
        ↓
TransaccionProcessor
        ↓
Transaccion
        ↓
JdbcBatchItemWriter
        ↓
transacciones_procesadas
```

El Processor valida:

* ID de transacción válido.
* Monto distinto de cero y mayor que cero.
* Tipo de transacción válido.
* Fecha válida.
* Normalización del tipo de transacción.
* Conversión de fecha desde String a LocalDate.

Se admiten los formatos:

```text
yyyy-MM-dd
yyyy/MM/dd
```

Los tipos válidos son:

```text
debito
credito
```

Los registros inválidos generan una `DatoInvalidoException` y son omitidos mediante la configuración fault-tolerant de Spring Batch.

### Step 2: generarResumenTransaccionesStep

Después de procesar las transacciones válidas se genera un resumen agrupado por fecha.

Se calcula:

* Cantidad de transacciones.
* Total de débitos.
* Total de créditos.
* Monto total procesado.

Los resultados se almacenan en:

```text
resumen_transacciones_diarias
```

---

## 6.2 Cálculo de Intereses Mensuales

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
InteresCsv
      ↓
InteresProcessor
      ↓
InteresCalculado
      ↓
JdbcBatchItemWriter
      ↓
intereses_calculados
```

El Processor valida:

* ID de cuenta válido.
* Nombre obligatorio.
* Saldo mayor que cero.
* Edad dentro de un rango válido.
* Tipo de cuenta permitido.
* Normalización del tipo de cuenta.

Los tipos procesados son:

```text
ahorro
prestamo
```

Los resultados almacenados incluyen:

* Saldo inicial.
* Tipo de cuenta.
* Tasa aplicada.
* Interés calculado.
* Saldo final.

### Tasas utilizadas

Las tasas utilizadas son configurables mediante `application.properties`:

```properties
banco.interes.ahorro=0.01
banco.interes.prestamo=0.02
```

Esto corresponde a:

```text
Ahorro   = 1%
Préstamo = 2%
```

Las tasas forman parte de la propuesta técnica de esta implementación, debido a que los datos de entrada no proporcionan porcentajes específicos.

El cálculo utilizado es:

```text
interes = saldoInicial × tasa
```

y posteriormente:

```text
saldoFinal = saldoInicial + interes
```

Los cálculos monetarios utilizan `BigDecimal`.

---

## 6.3 Generación de Estados de Cuenta Anuales

Job:

```text
estadosAnualesJob
```

Está compuesto por:

```text
estadosAnualesJob
│
├── procesarMovimientosAnualesStep
│
└── generarResumenAnualStep
```

### Step 1: procesarMovimientosAnualesStep

Lee:

```text
src/main/resources/input/cuentas_anuales.csv
```

Flujo:

```text
cuentas_anuales.csv
        ↓
FlatFileItemReader
        ↓
MovimientoAnualCsv
        ↓
MovimientoAnualProcessor
        ↓
MovimientoAnual
        ↓
JdbcBatchItemWriter
        ↓
movimientos_anuales
```

Las reglas aplicadas incluyen:

* Cuenta válida.
* Fecha válida.
* Monto distinto de cero.
* Tipo de movimiento válido.
* Normalización del tipo de movimiento.
* Normalización de descripciones.

Los tipos admitidos son:

```text
deposito
retiro
compra
```

Las reglas de monto son:

```text
deposito → monto positivo
retiro   → monto negativo
compra   → monto negativo
```

Si una descripción está vacía se normaliza como:

```text
SIN DESCRIPCION
```

### Step 2: generarResumenAnualStep

Los movimientos almacenados en PostgreSQL son agrupados por cuenta.

Por cada cuenta se calcula:

* Total de depósitos.
* Total de retiros.
* Total de compras.
* Saldo neto.
* Cantidad de movimientos.

El resultado se almacena en:

```text
estados_cuenta_anuales
```

Además, mediante un `CompositeItemWriter`, el resultado también se genera como archivo:

```text
output/estados_cuenta_anuales.csv
```

Esto permite disponer de una salida que puede ser utilizada como informe de auditoría.

---

# 7. Manejo de errores

El proyecto utiliza una excepción propia:

```text
DatoInvalidoException
```

Los Steps que procesan información proveniente de los archivos legacy están configurados como:

```text
faultTolerant
```

permitiendo omitir registros que presentan inconsistencias.

La estrategia utilizada es:

```text
Registro válido
      ↓
procesamiento normal
      ↓
persistencia

Registro inválido
      ↓
DatoInvalidoException
      ↓
Skip
      ↓
BatchSkipListener
      ↓
registro del error en consola
```

El límite configurado actualmente es:

```text
skipLimit = 100
```

Los registros omitidos son informados mediante `BatchSkipListener`, incluyendo el contenido del registro y el motivo del error.

Ejemplo:

```text
REGISTRO OMITIDO |
etapa=PROCESS |
registro=... |
motivo=...
```

Esto permite continuar el procesamiento del resto de los datos sin detener completamente el Job por un registro inconsistente.

---

# 8. Base de datos

La solución utiliza PostgreSQL.

Base requerida:

```text
banco_batch
```

Para crearla:

```sql
CREATE DATABASE banco_batch;
```

El proyecto utiliza dos grupos de tablas.

## Tablas internas de Spring Batch

Spring Batch genera tablas para almacenar metadatos de ejecución:

```text
batch_job_instance
batch_job_execution
batch_job_execution_params
batch_job_execution_context
batch_step_execution
batch_step_execution_context
```

Estas tablas permiten mantener información sobre:

* Jobs ejecutados.
* Instancias.
* Parámetros.
* Estados.
* Steps.
* Conteos de lectura y escritura.
* Registros omitidos.

## Tablas de negocio

La aplicación crea:

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

# 9. Configuración

El archivo:

```text
src/main/resources/application.properties
```

contiene:

```properties
spring.application.name=banco-batch

spring.datasource.url=jdbc:postgresql://localhost:5432/banco_batch
spring.datasource.username=postgres
spring.datasource.password=${DB_PASSWORD}

spring.batch.jdbc.initialize-schema=always
spring.batch.job.enabled=false

spring.sql.init.mode=always

logging.level.org.springframework.batch=INFO

banco.interes.ahorro=0.01
banco.interes.prestamo=0.02
```

La contraseña de PostgreSQL no está almacenada directamente en el repositorio.

Debe proporcionarse mediante la variable de entorno:

```text
DB_PASSWORD
```

---

# 10. Requisitos para ejecutar

Se requiere:

```text
Java 21
PostgreSQL
Git
```

El proyecto incluye Maven Wrapper, por lo que no es obligatorio instalar Maven de manera independiente.

Para comprobar Java:

```powershell
java -version
```

Para comprobar Maven:

```powershell
.\mvnw.cmd -version
```

---

# 11. Configurar contraseña PostgreSQL

En PowerShell:

```powershell
$env:DB_PASSWORD="CONTRASEÑA_POSTGRES"
```

Ejemplo:

```powershell
$env:DB_PASSWORD="miPassword"
```

La contraseña real no debe almacenarse en Git.

---

# 12. Compilar el proyecto

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

# 13. Generar el archivo JAR

Ejecutar:

```powershell
.\mvnw.cmd clean package -DskipTests
```

Esto genera:

```text
target/banco-batch-0.0.1-SNAPSHOT.jar
```

---

# 14. Ejecutar los Jobs

Debido a que existen varios Jobs, es necesario indicar cuál se desea ejecutar mediante:

```text
--spring.batch.job.name
```

Además, cada nueva ejecución debe utilizar un parámetro `run.id` diferente.

## Ejecutar transacciones

```powershell
java -jar target\banco-batch-0.0.1-SNAPSHOT.jar --spring.batch.job.enabled=true --spring.batch.job.name=transaccionesJob run.id=1
```

## Ejecutar intereses

```powershell
java -jar target\banco-batch-0.0.1-SNAPSHOT.jar --spring.batch.job.enabled=true --spring.batch.job.name=interesesJob run.id=1
```

## Ejecutar estados anuales

```powershell
java -jar target\banco-batch-0.0.1-SNAPSHOT.jar --spring.batch.job.enabled=true --spring.batch.job.name=estadosAnualesJob run.id=1
```

Para repetir un mismo Job se debe utilizar otro identificador, por ejemplo:

```text
run.id=2
run.id=3
run.id=4
```

---

# 15. Re-ejecución de procesos

Los Writers fueron diseñados para permitir nuevas ejecuciones sin duplicar información.

En las tablas donde existe una clave única se utilizan operaciones equivalentes a:

```sql
ON CONFLICT
DO UPDATE
```

o:

```sql
ON CONFLICT
DO NOTHING
```

Esto permite que un Job pueda volver a procesar los mismos archivos sin generar duplicados en las tablas de negocio.

---

# 16. Resultados generados

## Transacciones

Los registros válidos se almacenan en:

```text
transacciones_procesadas
```

El resumen diario se almacena en:

```text
resumen_transacciones_diarias
```

## Intereses

Los resultados se almacenan en:

```text
intereses_calculados
```

Incluyen:

```text
saldo_inicial
tasa
interes
saldo_final
```

## Estados anuales

Los movimientos normalizados quedan en:

```text
movimientos_anuales
```

Los resúmenes por cuenta quedan en:

```text
estados_cuenta_anuales
```

También se genera:

```text
output/estados_cuenta_anuales.csv
```

---

# 17. Consultas de verificación

## Transacciones procesadas

```sql
SELECT *
FROM transacciones_procesadas
ORDER BY id;
```

## Resumen de transacciones

```sql
SELECT *
FROM resumen_transacciones_diarias
ORDER BY fecha;
```

## Intereses

```sql
SELECT *
FROM intereses_calculados
ORDER BY cuenta_id;
```

## Movimientos anuales

```sql
SELECT *
FROM movimientos_anuales
ORDER BY cuenta_id, fecha;
```

## Estados anuales

```sql
SELECT *
FROM estados_cuenta_anuales
ORDER BY cuenta_id;
```

---

# 18. Verificación de ejecución de Steps

Para comprobar las ejecuciones registradas por Spring Batch:

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

Esto permite verificar:

* Estado del Step.
* Cantidad de registros leídos.
* Cantidad de registros escritos.
* Cantidad de registros omitidos durante el procesamiento.

---

# 19. Propuesta técnica

La solución utiliza una arquitectura Batch basada en componentes independientes para separar claramente las responsabilidades de lectura, procesamiento y escritura.

Las principales decisiones técnicas fueron:

* Java 21 como versión de ejecución.
* Spring Batch para orquestar Jobs y Steps.
* PostgreSQL como base de datos relacional.
* JDBC para persistencia.
* `BigDecimal` para cálculos monetarios.
* Separación entre modelos CSV y modelos procesados.
* Validación mediante `ItemProcessor`.
* Manejo de inconsistencias mediante Skip.
* `BatchSkipListener` para registrar datos descartados.
* Jobs re-ejecutables mediante operaciones `ON CONFLICT`.
* Variables de entorno para evitar almacenar credenciales.
* Generación de resultados tanto en PostgreSQL como en archivos cuando corresponde.
* Agrupaciones mediante SQL para generar los resúmenes diarios y anuales.

---

# 20. Supuestos técnicos

Las instrucciones del ejercicio no proporcionan tasas específicas para el cálculo de intereses.

Por este motivo se definieron como valores configurables:

```text
Ahorro   = 1%
Préstamo = 2%
```

Estos valores pueden modificarse sin recompilar la aplicación mediante:

```properties
banco.interes.ahorro
banco.interes.prestamo
```

Para préstamos, el saldo se interpreta como el monto sobre el cual se aplica el interés, por lo que el interés calculado se suma al saldo inicial.

También se considera que:

```text
deposito → monto positivo
retiro   → monto negativo
compra   → monto negativo
```

Los movimientos con monto cero son considerados inconsistentes.

---

# 21. Evidencias

Las evidencias de ejecución del proyecto se almacenan en:

```text
evidencias/
```

Incluyen resultados de:

* Ejecución de `transaccionesJob`.
* Ejecución de `interesesJob`.
* Ejecución de `estadosAnualesJob`.
* Estados `COMPLETED`.
* Registros omitidos.
* Conteos de lectura, escritura y skips.
* Resultados persistidos en PostgreSQL.
* Archivo anual generado.

---

# 22. Resultado final

La solución implementa los tres procesos batch requeridos para la modernización de los procesos legacy del Banco XYZ:

```text
Reporte de Transacciones Diarias
Cálculo de Intereses Mensuales
Generación de Estados de Cuenta Anuales
```

Cada proceso utiliza componentes de Spring Batch para realizar lectura, procesamiento y escritura de datos, incorporando además validaciones, manejo de errores, persistencia y generación de resultados.
