-- ============================================================
-- SpeedFast - Semana 7 (JDBC + MySQL)
-- Script de estructura: base de datos speedfast_db y sus 3 tablas.
-- Ejecutar completo en MySQL Workbench (o por consola: mysql -u root -p < script_estructura.sql)
-- ============================================================

CREATE DATABASE IF NOT EXISTS speedfast_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE speedfast_db;

-- Un repartidor puede realizar muchas entregas.
CREATE TABLE IF NOT EXISTS repartidor (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL
);

-- Un pedido puede tener una o varias entregas (intentos / etapas).
CREATE TABLE IF NOT EXISTS pedido (
    id INT AUTO_INCREMENT PRIMARY KEY,
    direccion VARCHAR(150) NOT NULL,
    tipo VARCHAR(30) NOT NULL,      -- COMIDA | ENCOMIENDA | EXPRESS
    estado VARCHAR(20) NOT NULL     -- PENDIENTE | EN_REPARTO | ENTREGADO
);

-- Cada entrega se asocia a un pedido y a un repartidor.
CREATE TABLE IF NOT EXISTS entrega (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_pedido INT NOT NULL,
    id_repartidor INT NOT NULL,
    fecha DATE NOT NULL,
    hora TIME NOT NULL,
    FOREIGN KEY (id_pedido) REFERENCES pedido(id),
    FOREIGN KEY (id_repartidor) REFERENCES repartidor(id)
);

-- Verificación: debe mostrar las 3 tablas y las 2 claves foráneas de "entrega".
SHOW TABLES;
SELECT TABLE_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'speedfast_db' AND REFERENCED_TABLE_NAME IS NOT NULL;
