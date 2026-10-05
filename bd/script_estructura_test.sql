-- ============================================================
-- SpeedFast - Semana 7 (JDBC + MySQL)
-- Base de datos EXCLUSIVA para las pruebas unitarias de los DAO: speedfast_test.
-- Tiene las mismas 3 tablas que speedfast_db, pero las pruebas la vacían antes de cada test,
-- así que no se mezcla con los datos reales de la aplicación.
-- Ejecutar completo en MySQL Workbench (o: mysql -u root -p < script_estructura_test.sql)
-- ============================================================

CREATE DATABASE IF NOT EXISTS speedfast_test
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE speedfast_test;

CREATE TABLE IF NOT EXISTS repartidor (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS pedido (
    id INT AUTO_INCREMENT PRIMARY KEY,
    direccion VARCHAR(150) NOT NULL,
    tipo VARCHAR(30) NOT NULL,      -- COMIDA | ENCOMIENDA | EXPRESS
    estado VARCHAR(20) NOT NULL     -- PENDIENTE | EN_REPARTO | ENTREGADO
);

CREATE TABLE IF NOT EXISTS entrega (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_pedido INT NOT NULL,
    id_repartidor INT NOT NULL,
    fecha DATE NOT NULL,
    hora TIME NOT NULL,
    FOREIGN KEY (id_pedido) REFERENCES pedido(id),
    FOREIGN KEY (id_repartidor) REFERENCES repartidor(id)
);

SHOW TABLES;
