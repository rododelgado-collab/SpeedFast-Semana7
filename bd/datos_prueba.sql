-- Datos de prueba OPCIONALES (la aplicación también permite registrarlos desde la interfaz).
USE speedfast_db;

INSERT INTO repartidor (nombre) VALUES ('Juan'), ('María'), ('Pedro');

INSERT INTO pedido (direccion, tipo, estado) VALUES
    ('Av. Providencia 1234, Providencia', 'COMIDA',     'PENDIENTE'),
    ('Calle Huérfanos 850, Santiago',     'ENCOMIENDA', 'PENDIENTE'),
    ('Av. Apoquindo 4500, Las Condes',    'EXPRESS',    'PENDIENTE');

SELECT * FROM repartidor;
SELECT * FROM pedido;
