# Sistema Bancario con Patrón BFF (Backend for Frontend)

## Objetivo

Implementar el patrón arquitectónico **Backend for Frontend (BFF)** para Banco XYZ, utilizando un Backend Central como fuente de datos y tres BFF independientes orientados a los canales:

- Web
- Mobile
- Cajero Automático

Cada BFF expone únicamente la información necesaria para su cliente, incorporando autenticación y autorización mediante JWT, validación específica por canal y HTTPS con certificados locales.

El proyecto continúa el trabajo realizado en las semanas anteriores a partir del modelo de datos del repositorio legacy `KariVillagran/bank_legacy_data`.

---

## Propuesta técnica

Para resolver los requerimientos de los distintos clientes de Banco XYZ se propone implementar una arquitectura basada en el patrón **Backend for Frontend (BFF)**.

La solución utiliza un **Backend Central** encargado de concentrar el acceso a los datos, la lógica bancaria, la autenticación y la persistencia, junto con **tres BFF independientes**, uno por cada tipo de cliente:

- **BFF Web:** orientado a navegadores y encargado de entregar información completa de la cuenta.
- **BFF Mobile:** orientado a dispositivos móviles y encargado de entregar una respuesta reducida con la información esencial.
- **BFF Cajero:** orientado a cajeros automáticos y encargado de entregar únicamente información crítica, además de permitir operaciones como consulta de saldo y retiro.

La arquitectura busca evitar que todos los clientes dependan directamente de una misma API genérica. Cada BFF adapta las respuestas según las necesidades de su canal, reduciendo información innecesaria y permitiendo aplicar reglas de seguridad específicas.

### Decisiones técnicas principales

La propuesta considera las siguientes decisiones:

- utilizar proyectos Spring Boot independientes para cada BFF;
- mantener el acceso directo a PostgreSQL únicamente en el Backend Central;
- utilizar DTO específicos para adaptar las respuestas de cada canal;
- utilizar JWT para autenticación y autorización;
- incorporar el canal `WEB`, `MOBILE` o `CAJERO` dentro del token;
- impedir que un token emitido para un canal sea utilizado en otro BFF;
- utilizar roles `CLIENTE` y `EMPLEADO`;
- proteger los tres BFF mediante HTTPS;
- utilizar certificados autofirmados para el entorno local de desarrollo;
- mantener contraseñas y secretos mediante variables de entorno;
- implementar el retiro en el BFF Cajero como operación crítica;
- validar en el Backend Central la propiedad de la cuenta antes de modificar el saldo;
- utilizar PostgreSQL ejecutado mediante Docker para facilitar la ejecución y reproducibilidad;
- mantener centralizada la lógica bancaria para evitar duplicarla en cada BFF.

### Beneficios de la propuesta

La separación en tres BFF permite:

- reducir el acoplamiento entre los clientes y el Backend Central;
- entregar únicamente los datos que cada cliente necesita;
- reducir el tamaño de las respuestas;
- aplicar seguridad específica según el canal;
- mantener una arquitectura modular;
- facilitar la incorporación futura de nuevos canales;
- centralizar la lógica bancaria y la persistencia;
- permitir que Web, Mobile y Cajero evolucionen de manera independiente.

---

## Arquitectura

```text
Cliente Web
    |
    | HTTPS :8084
    v
BFF Web
    |
    +------------------------+
                             |
Cliente Mobile               |
    |                        |
    | HTTPS :8083            |
    v                        |
BFF Mobile                   |
    |                        |
    +------------------------+----> Backend Central :8081
                             |              |
Cliente Cajero               |              |
    |                        |              v
    | HTTPS :8082            |       PostgreSQL Docker
    v                        |           :5433
BFF Cajero                   |
    |                        |
    +------------------------+
```

### Componentes

### Backend Central

Es la fuente central de información y lógica bancaria.

Responsabilidades principales:

- acceso a PostgreSQL;
- autenticación de usuarios;
- generación y validación de JWT;
- autorización por rol;
- autorización por propiedad de cuenta;
- consulta de cuentas;
- consulta de saldo;
- operación de retiro;
- persistencia de los cambios.

### BFF Web

Entrega información completa de la cuenta para un navegador.

### BFF Mobile

Reduce la respuesta y entrega solamente la información necesaria para una aplicación móvil.

### BFF Cajero

Entrega la respuesta mínima necesaria para un cajero automático y permite realizar operaciones críticas como consulta de saldo y retiro.

---

## Puertos

| Servicio | Puerto | Protocolo | URL base |
|---|---:|---|---|
| PostgreSQL Docker | `5433` | PostgreSQL | `localhost:5433` |
| Backend Central | `8081` | HTTP local | `http://localhost:8081` |
| BFF Cajero | `8082` | HTTPS | `https://localhost:8082` |
| BFF Mobile | `8083` | HTTPS | `https://localhost:8083` |
| BFF Web | `8084` | HTTPS | `https://localhost:8084` |

Los tres BFF exponen HTTPS al cliente mediante certificados autofirmados para el entorno local.

La comunicación BFF → Backend Central utiliza HTTP sobre `localhost` en esta implementación académica.

---

## Optimización por canal

Una de las responsabilidades principales del patrón BFF es adaptar la respuesta a las necesidades de cada cliente.

| Canal | Información entregada |
|---|---|
| Web | `cuentaIdLegacy`, `nombre`, `saldo`, `edad`, `tipo`, `interes`, `saldoFinal` |
| Mobile | `nombre`, `saldo`, `tipoCuenta` |
| Cajero - saldo | `saldo` |
| Cajero - retiro | `montoRetirado`, `saldoDisponible` |

### Ejemplo Web

```json
{
  "cuentaIdLegacy": 137,
  "nombre": "Steve Rogers",
  "saldo": 10000.00,
  "edad": 25,
  "tipo": "ahorro",
  "interes": 500.00,
  "saldoFinal": 10500.00
}
```

### Ejemplo Mobile

```json
{
  "nombre": "Steve Rogers",
  "saldo": 10500.00,
  "tipoCuenta": "ahorro"
}
```

### Ejemplo Cajero

```json
{
  "saldo": 10500.00
}
```

De esta forma, Web recibe información completa, Mobile una respuesta resumida y Cajero únicamente los datos críticos necesarios para la operación.

Esto evita enviar información innecesaria a clientes que no la requieren.

---

## Seguridad

### JWT

La autenticación se realiza en el Backend Central mediante:

```text
POST /api/auth/login
```

Ejemplo:

```json
{
  "username": "steve",
  "password": "Steve2026!",
  "canal": "WEB"
}
```

Los canales permitidos son:

```text
WEB
MOBILE
CAJERO
```

El JWT generado contiene información como:

```text
sub
rol
cuentaIdLegacy
canal
iat
exp
```

La duración configurada del token es de una hora.

---

### Autorización específica por canal

Cada BFF valida que el token pertenezca al canal correcto.

Ejemplos:

```text
Token WEB    -> BFF Web     -> permitido
Token WEB    -> BFF Mobile  -> 403
Token WEB    -> BFF Cajero  -> 403

Token MOBILE -> BFF Mobile  -> permitido
Token MOBILE -> BFF Web     -> 403

Token CAJERO -> BFF Cajero  -> permitido
```

Esto evita reutilizar un token válido en un canal diferente.

---

### Roles

Los roles utilizados son:

```text
CLIENTE
EMPLEADO
```

Las consultas pueden autorizar clientes o empleados dependiendo del endpoint.

La operación de retiro del Cajero exige:

```text
ROLE_CLIENTE
CANAL_CAJERO
```

Además, el Backend Central verifica que la cuenta utilizada para el retiro pertenezca al cliente autenticado.

---

### HTTPS

Los tres BFF utilizan HTTPS:

```text
https://localhost:8082
https://localhost:8083
https://localhost:8084
```

Los certificados utilizados durante el desarrollo son autofirmados.

Los archivos `.p12` no se incluyen en Git debido a que contienen claves privadas.

Deben generarse localmente antes de ejecutar los BFF.

---

## Endpoints principales

### Autenticación

#### Login

```http
POST http://localhost:8081/api/auth/login
```

Body:

```json
{
  "username": "steve",
  "password": "Steve2026!",
  "canal": "WEB"
}
```

Para obtener un token de otro canal se debe cambiar el valor de `canal` por:

```text
MOBILE
```

o:

```text
CAJERO
```

---

### BFF Web

#### Obtener cuenta completa

```http
GET https://localhost:8084/bff-web/cuentas/{id}
```

Requiere:

```text
Bearer Token
CANAL_WEB
ROLE_CLIENTE o ROLE_EMPLEADO
```

---

### BFF Mobile

#### Consultar información resumida

```http
GET https://localhost:8083/bff-mobile/cuentas/{id}/saldo
```

Requiere:

```text
Bearer Token
CANAL_MOBILE
ROLE_CLIENTE o ROLE_EMPLEADO
```

---

### BFF Cajero

#### Consultar saldo

```http
GET https://localhost:8082/bff-cajero/cuentas/{id}/saldo
```

Requiere:

```text
Bearer Token
CANAL_CAJERO
```

#### Retirar dinero

```http
POST https://localhost:8082/bff-cajero/cuentas/{id}/retiros
```

Body:

```json
{
  "monto": 500
}
```

Requiere:

```text
Bearer Token
ROLE_CLIENTE
CANAL_CAJERO
```

Respuesta optimizada:

```json
{
  "montoRetirado": 500,
  "saldoDisponible": 10000.00
}
```

---

## Flujo de retiro

```text
Cliente Cajero
      |
      | HTTPS + JWT CAJERO
      v
BFF Cajero
      |
      | transforma DTO
      v
Backend Central
      |
      | valida usuario
      | valida propiedad
      | valida monto
      | valida tipo de cuenta
      | valida fondos
      v
PostgreSQL
      |
      | actualiza saldo
      v
Backend Central
      |
      v
BFF Cajero
      |
      | respuesta optimizada
      v
Cliente Cajero
```

El método de retiro en el Backend Central utiliza `@Transactional` para mantener la consistencia de la operación.

También se valida que:

```text
monto > 0
cuenta = ahorro
saldo >= monto solicitado
cuenta pertenece al usuario
```

La actualización se persiste en PostgreSQL, por lo que los demás BFF pueden consultar posteriormente el nuevo saldo.

---

## Manejo de errores

Los servicios utilizan manejo centralizado de excepciones mediante:

```text
@RestControllerAdvice
```

Respuestas principales:

| Código | Situación |
|---:|---|
| `200` | operación exitosa |
| `400` | monto inválido, fondos insuficientes u operación no permitida |
| `401` | usuario no autenticado o token inválido |
| `403` | rol, canal o propiedad de cuenta no autorizados |
| `404` | recurso no encontrado |
| `503` | Backend Central no disponible |

Ejemplo sin autenticación:

```json
{
  "mensaje": "Debes iniciar sesión para acceder a este recurso"
}
```

Ejemplo de canal incorrecto:

```json
{
  "mensaje": "No tienes permisos para acceder a este recurso"
}
```

---

## Base de datos

La base de datos se ejecuta localmente mediante Docker con:

```text
PostgreSQL 16
```

El archivo utilizado es:

```text
Banco-central/banco-central-xyz/docker-compose.yml
```

Puerto local:

```text
5433
```

Base:

```text
banco_xyz
```

Usuario:

```text
banco_admin
```

La contraseña no está almacenada directamente en el repositorio y se obtiene mediante:

```text
POSTGRES_PASSWORD
SPRING_DATASOURCE_PASSWORD
```

---

## Datos de demostración

El Backend Central contiene un `DataInitializer` que permite ejecutar la demostración en una base nueva sin insertar manualmente la cuenta de prueba.

Si no existe la cuenta con:

```text
cuenta_id_legacy = 137
```

se crea una cuenta local de demostración para:

```text
Steve Rogers
tipo: ahorro
saldo inicial: 10000.00
interés: 500.00
saldo final: 10500.00
```

El inicializador solo crea la cuenta si no existe.

Por lo tanto, un retiro persistido no se sobrescribe al reiniciar la aplicación.

Este registro se utiliza como dato determinístico para ejecutar y evaluar la arquitectura. El modelo general del proyecto continúa basado en los datos legacy utilizados durante el desarrollo del curso.

---

## Requisitos

Para ejecutar el proyecto localmente se necesita:

```text
Java 17 o superior
Docker
Docker Compose
keytool
Postman
Git
```

Los proyectos incluyen Maven Wrapper, por lo que no es obligatorio instalar Maven de forma global.

---

## Variables de entorno

No se almacenan contraseñas ni claves directamente en `application.properties`.

### Backend Central

PowerShell:

```powershell
$env:POSTGRES_PASSWORD="<PASSWORD_POSTGRES>"
$env:SPRING_DATASOURCE_PASSWORD="<PASSWORD_POSTGRES>"
$env:JWT_SECRET="<JWT_SECRET>"
```

### BFF Web

```powershell
$env:JWT_SECRET="<JWT_SECRET>"
$env:SSL_KEYSTORE_PASSWORD="<PASSWORD_CERTIFICADO_WEB>"
```

### BFF Mobile

```powershell
$env:JWT_SECRET="<JWT_SECRET>"
$env:SSL_KEYSTORE_PASSWORD="<PASSWORD_CERTIFICADO_MOBILE>"
```

### BFF Cajero

```powershell
$env:JWT_SECRET="<JWT_SECRET>"
$env:SSL_KEYSTORE_PASSWORD="<PASSWORD_CERTIFICADO_CAJERO>"
```

Todos los servicios deben utilizar el mismo `JWT_SECRET`.

---

## Generación de certificados

Los certificados `.p12` están excluidos mediante `.gitignore`.

### BFF Web

Desde la raíz de `bff-web`:

```powershell
New-Item -ItemType Directory -Force ".\src\main\resources\certs"

keytool -genkeypair `
-alias bff-web `
-keyalg RSA `
-keysize 2048 `
-storetype PKCS12 `
-keystore ".\src\main\resources\certs\bff-web.p12" `
-validity 3650 `
-storepass "<PASSWORD_CERTIFICADO_WEB>" `
-keypass "<PASSWORD_CERTIFICADO_WEB>" `
-dname "CN=localhost, OU=Backend III, O=Duoc UC, L=Santiago, ST=RM, C=CL" `
-ext "SAN=dns:localhost,ip:127.0.0.1"
```

### BFF Mobile

Desde la raíz de `bff-mobile`:

```powershell
New-Item -ItemType Directory -Force ".\src\main\resources\certs"

keytool -genkeypair `
-alias bff-mobile `
-keyalg RSA `
-keysize 2048 `
-storetype PKCS12 `
-keystore ".\src\main\resources\certs\bff-mobile.p12" `
-validity 3650 `
-storepass "<PASSWORD_CERTIFICADO_MOBILE>" `
-keypass "<PASSWORD_CERTIFICADO_MOBILE>" `
-dname "CN=localhost, OU=Backend III, O=Duoc UC, L=Santiago, ST=RM, C=CL" `
-ext "SAN=dns:localhost,ip:127.0.0.1"
```

### BFF Cajero

Desde la raíz de `bff-cajeros`:

```powershell
New-Item -ItemType Directory -Force ".\src\main\resources\certs"

keytool -genkeypair `
-alias bff-cajeros `
-keyalg RSA `
-keysize 2048 `
-storetype PKCS12 `
-keystore ".\src\main\resources\certs\bff-cajeros.p12" `
-validity 3650 `
-storepass "<PASSWORD_CERTIFICADO_CAJERO>" `
-keypass "<PASSWORD_CERTIFICADO_CAJERO>" `
-dname "CN=localhost, OU=Backend III, O=Duoc UC, L=Santiago, ST=RM, C=CL" `
-ext "SAN=dns:localhost,ip:127.0.0.1"
```

---

## Ejecución

### 1. PostgreSQL

Desde:

```text
Banco-central/banco-central-xyz
```

configurar:

```powershell
$env:POSTGRES_PASSWORD="<PASSWORD_POSTGRES>"
```

y ejecutar:

```powershell
docker compose up -d
```

Verificar:

```powershell
docker ps
```

El contenedor esperado es:

```text
banco-xyz-postgres
```

---

### 2. Backend Central

Desde:

```text
Banco-central/banco-central-xyz
```

configurar:

```powershell
$env:SPRING_DATASOURCE_PASSWORD="<PASSWORD_POSTGRES>"
$env:JWT_SECRET="<JWT_SECRET>"
```

Ejecutar:

```powershell
.\mvnw.cmd spring-boot:run
```

Servicio:

```text
http://localhost:8081
```

---

### 3. BFF Cajero

Desde:

```text
bff-cajeros
```

configurar:

```powershell
$env:JWT_SECRET="<JWT_SECRET>"
$env:SSL_KEYSTORE_PASSWORD="<PASSWORD_CERTIFICADO_CAJERO>"
```

Ejecutar:

```powershell
.\mvnw.cmd spring-boot:run
```

Servicio:

```text
https://localhost:8082
```

---

### 4. BFF Mobile

Desde:

```text
bff-mobile
```

configurar:

```powershell
$env:JWT_SECRET="<JWT_SECRET>"
$env:SSL_KEYSTORE_PASSWORD="<PASSWORD_CERTIFICADO_MOBILE>"
```

Ejecutar:

```powershell
.\mvnw.cmd spring-boot:run
```

Servicio:

```text
https://localhost:8083
```

---

### 5. BFF Web

Desde:

```text
bff-web
```

configurar:

```powershell
$env:JWT_SECRET="<JWT_SECRET>"
$env:SSL_KEYSTORE_PASSWORD="<PASSWORD_CERTIFICADO_WEB>"
```

Ejecutar:

```powershell
.\mvnw.cmd spring-boot:run
```

Servicio:

```text
https://localhost:8084
```

---

## Pruebas con Postman

Para los certificados autofirmados utilizados localmente:

```text
Postman
Settings
General
SSL certificate verification
OFF
```

Esto se utiliza únicamente durante las pruebas locales.

Las pruebas realizadas incluyen:

```text
Login WEB                       -> 200
BFF Web con token WEB           -> 200

Login MOBILE                    -> 200
BFF Mobile con token MOBILE     -> 200

Login CAJERO                    -> 200
Saldo Cajero                    -> 200
Retiro Cajero                   -> 200
Saldo después del retiro        -> 200

Cajero sin token                -> 401
Token WEB en Cajero             -> 403
Token WEB en Mobile             -> 403
Token MOBILE en Web             -> 403
Retiro Cajero con token WEB     -> 403
```

Estas pruebas demuestran tanto los casos exitosos como los controles de autenticación y autorización específicos por canal.

---

## Evidencias

Las evidencias de ejecución se encuentran en:

```text
evidencias/
```

Incluyen:

```text
01 - PostgreSQL ejecutándose con Docker
02 - Backend Central iniciado
03 - BFF Cajero ejecutándose con HTTPS
04 - BFF Mobile ejecutándose con HTTPS
05 - BFF Web ejecutándose con HTTPS
06 - Autenticación WEB
07 - Respuesta completa BFF Web
08 - Autenticación MOBILE
09 - Respuesta optimizada BFF Mobile
10 - Autenticación CAJERO
11 - Consulta de saldo BFF Cajero
12 - Retiro exitoso BFF Cajero
13 - Saldo actualizado después del retiro
14 - Respuesta 401 sin token
15 - Token WEB rechazado por BFF Cajero
16 - Token WEB rechazado por BFF Mobile
17 - Token MOBILE rechazado por BFF Web
18 - Retiro Cajero rechazado con token WEB
19 - Mobile reflejando el saldo actualizado
20 - Colección completa de pruebas Postman
```

---

## Estructura del proyecto

```text
Exp2_S5_Pedro_Breit/
|
+-- Banco-central/
|   +-- banco-central-xyz/
|
+-- bff-web/
|
+-- bff-mobile/
|
+-- bff-cajeros/
|
+-- evidencias/
|
+-- .gitignore
|
+-- README.md
```

Cada BFF mantiene responsabilidades separadas mediante paquetes como:

```text
config
security
controller
service
client
dto
exception
```

El Backend Central utiliza adicionalmente:

```text
entity
repository
mapper
```

El acceso directo a entidades y repositorios está concentrado en el Backend Central.

---

## Modularidad y escalabilidad

La separación de cada canal en un proyecto independiente permite modificar un BFF sin modificar directamente los demás.

Por ejemplo:

```text
Cambios exclusivos de Web    -> bff-web
Cambios exclusivos de Mobile -> bff-mobile
Cambios exclusivos de Cajero -> bff-cajeros
```

La lógica bancaria y el acceso a datos permanecen centralizados en el Backend Central.

La arquitectura permite incorporar en el futuro nuevos canales, por ejemplo:

```text
BFF Empresas
BFF Sucursales
BFF Integraciones
```

sin obligar a reutilizar las respuestas diseñadas para Web, Mobile o Cajero.

---

## Consideraciones de seguridad

Los secretos se gestionan mediante variables de entorno.

Los archivos que contienen claves privadas de certificados no se almacenan en Git.

El `.gitignore` excluye:

```text
target/
.env
certificados .p12
archivos .jks
logs
archivos generados por IDE
```

Durante las pruebas se verificaron respuestas:

```text
401 Unauthorized
403 Forbidden
```

para demostrar que los endpoints no permiten acceso sin autenticación ni mediante tokens emitidos para canales incorrectos.

Para un entorno productivo se recomienda:

- reemplazar los certificados autofirmados por certificados emitidos por una autoridad certificadora;
- utilizar un gestor seguro de secretos;
- proteger también mediante TLS las comunicaciones internas entre servicios;
- utilizar claves JWT administradas mediante infraestructura segura;
- incorporar monitoreo y auditoría de operaciones críticas.

---

## Limitaciones y mejoras futuras

La solución corresponde a un entorno académico local.

Actualmente:

- los BFF utilizan HTTPS con certificados autofirmados;
- la comunicación BFF → Backend Central utiliza HTTP sobre `localhost`;
- se utiliza un `JWT_SECRET` compartido para firma y validación;
- se utiliza una cuenta determinística de demostración para facilitar la evaluación.

Como mejoras futuras se propone:

- utilizar HTTPS también entre los BFF y el Backend Central;
- utilizar certificados emitidos por una CA;
- utilizar firma JWT asimétrica, donde el Backend Central conserve la clave privada y los BFF solo conozcan la clave pública;
- incorporar auditoría de retiros;
- incorporar pruebas automatizadas de integración;
- implementar despliegue mediante contenedores para todos los servicios.

---

## Resultado

La solución implementa tres BFF independientes y optimizados para cada tipo de cliente:

```text
Web    -> información completa
Mobile -> información resumida
Cajero -> información mínima + operaciones críticas
```

Además incorpora:

```text
JWT
roles
validación por canal
autorización por propiedad
HTTPS
certificados
PostgreSQL
Docker
manejo de errores
DTO específicos por canal
retiro transaccional
persistencia de saldo
arquitectura modular
evidencias de ejecución
```

De esta manera, cada cliente recibe únicamente los datos y operaciones que necesita sin acoplarse directamente al modelo interno del Backend Central.
