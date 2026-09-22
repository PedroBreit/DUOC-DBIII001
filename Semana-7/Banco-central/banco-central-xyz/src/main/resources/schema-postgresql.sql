-- =========================================================
-- USUARIOS DE PRUEBA
-- =========================================================

DROP TABLE IF EXISTS usuarios;


-- =========================================================
-- TABLA USUARIOS
-- =========================================================

CREATE TABLE usuarios (

    id BIGSERIAL PRIMARY KEY,

    username VARCHAR(50) NOT NULL UNIQUE,

    password_hash VARCHAR(255) NOT NULL,

    rol VARCHAR(20) NOT NULL,

    cuenta_id_legacy BIGINT
);


-- =========================================================
-- CLIENTE DE PRUEBA
-- =========================================================
-- Usuario: steve
-- Password: Steve2026!
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
-- Password: Maria2026!
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