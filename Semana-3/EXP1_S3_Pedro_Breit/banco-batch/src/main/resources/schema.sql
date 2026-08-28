CREATE TABLE IF NOT EXISTS transacciones_procesadas (
    id BIGINT PRIMARY KEY,
    fecha DATE NOT NULL,
    monto NUMERIC(15,2) NOT NULL,
    tipo VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS intereses_calculados (
    cuenta_id BIGINT PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    saldo_inicial NUMERIC(15,2) NOT NULL,
    edad INTEGER NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    tasa NUMERIC(10,4) NOT NULL,
    interes NUMERIC(15,2) NOT NULL,
    saldo_final NUMERIC(15,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS movimientos_anuales (
    id BIGSERIAL PRIMARY KEY,
    cuenta_id BIGINT NOT NULL,
    fecha DATE NOT NULL,
    transaccion VARCHAR(30) NOT NULL,
    monto NUMERIC(15,2) NOT NULL,
    descripcion VARCHAR(255) NOT NULL,
    CONSTRAINT uk_movimiento_anual
        UNIQUE (cuenta_id, fecha, transaccion, monto, descripcion)
);

CREATE TABLE IF NOT EXISTS estados_cuenta_anuales (
    cuenta_id BIGINT PRIMARY KEY,
    total_depositos NUMERIC(15,2) NOT NULL,
    total_retiros NUMERIC(15,2) NOT NULL,
    total_compras NUMERIC(15,2) NOT NULL,
    saldo_neto NUMERIC(15,2) NOT NULL,
    cantidad_movimientos INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS resumen_transacciones_diarias (
    fecha DATE PRIMARY KEY,
    cantidad_transacciones INTEGER NOT NULL,
    total_debitos NUMERIC(15,2) NOT NULL,
    total_creditos NUMERIC(15,2) NOT NULL,
    monto_total NUMERIC(15,2) NOT NULL
);
