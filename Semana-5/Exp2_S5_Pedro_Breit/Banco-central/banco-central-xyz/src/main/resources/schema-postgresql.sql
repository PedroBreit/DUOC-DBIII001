-- =========================================================
-- USUARIOS DE PRUEBA
-- =========================================================

-- Se elimina solamente la tabla de usuarios para poder
-- recrear las credenciales de prueba de forma consistente.
DROP TABLE IF EXISTS usuarios;


-- =========================================================
-- TABLA USUARIOS
-- =========================================================

CREATE TABLE usuarios (

    id BIGSERIAL PRIMARY KEY,

    username VARCHAR(50) NOT NULL UNIQUE,

    -- La contraseña nunca se almacena directamente.
    -- Se guarda utilizando BCrypt.
    password_hash VARCHAR(255) NOT NULL,

    rol VARCHAR(20) NOT NULL,

    -- Cuenta legacy asociada al cliente.
    -- Los empleados no necesitan una cuenta específica.
    cuenta_id_legacy BIGINT
);


-- =========================================================
-- CLIENTE DE PRUEBA
-- =========================================================
-- Usuario: steve
-- Contraseña de desarrollo: Steve2026!
-- Rol: CLIENTE
-- Cuenta legacy: 137

INSERT INTO usuarios (
    username,
    password_hash,
    rol,
    cuenta_id_legacy
)
VALUES (
    'steve',
    '$2y$10$cViAgK/f0Gv9A3lUAOdJOOgZhjmybC4GCOhNzsD7as79im8Nso3K.',
    'CLIENTE',
    137
);


-- =========================================================
-- EMPLEADO DE PRUEBA
-- =========================================================
-- Usuario: maria
-- Contraseña de desarrollo: Maria2026!
-- Rol: EMPLEADO

INSERT INTO usuarios (
    username,
    password_hash,
    rol,
    cuenta_id_legacy
)
VALUES (
    'maria',
    '$2y$10$sv9m/AbMZ5Quy.1fqqcfNeECPJ6JQQ5bW6qJgpWma83ieJrZRdNtO',
    'EMPLEADO',
    NULL
);