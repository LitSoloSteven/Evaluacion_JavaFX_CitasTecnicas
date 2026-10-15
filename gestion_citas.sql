-- =====================================================================
-- Sistema de Gestión de Citas Técnicas - TecnoAsistencia Nicaragua S.A.
-- Script de creación de base de datos
-- PostgreSQL 15+
-- =====================================================================

-- Ejecutar conectado a la base 'postgres' como superusuario:
-- DROP DATABASE IF EXISTS gestion_citas;
-- CREATE DATABASE gestion_citas
--     WITH ENCODING 'UTF8'
--     LC_COLLATE 'es_NI.UTF-8'
--     LC_CTYPE   'es_NI.UTF-8'
--     TEMPLATE = template0;
-- \c gestion_citas

-- Si el locale no está disponible, usar:
-- CREATE DATABASE gestion_citas WITH ENCODING 'UTF8';

-- ---------------------------------------------------------------------
-- Tabla: cliente
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cliente (
                                       id_cliente  SERIAL       PRIMARY KEY,
                                       nombres     VARCHAR(80)  NOT NULL,
    apellidos   VARCHAR(80)  NOT NULL,
    telefono    VARCHAR(20)  NOT NULL,
    correo      VARCHAR(120) NOT NULL,
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_cliente_correo UNIQUE (correo),
    CONSTRAINT chk_cliente_correo CHECK (correo ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    CONSTRAINT chk_cliente_nombres   CHECK (length(trim(nombres))   > 0),
    CONSTRAINT chk_cliente_apellidos CHECK (length(trim(apellidos)) > 0),
    CONSTRAINT chk_cliente_telefono  CHECK (length(trim(telefono))  > 0)
    );

-- Índice único insensible a mayúsculas para respaldar el DAO:
-- evita duplicados como 'Ana@x.com' vs 'ana@x.com'
CREATE UNIQUE INDEX IF NOT EXISTS uq_cliente_correo_lower
    ON cliente (LOWER(correo));

-- ---------------------------------------------------------------------
-- Tabla: cita_tecnica
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cita_tecnica (
                                            id_cita        SERIAL       PRIMARY KEY,
                                            id_cliente     INTEGER      NOT NULL,
                                            fecha          DATE         NOT NULL,
                                            hora           TIME         NOT NULL,
                                            tipo_servicio  VARCHAR(80)  NOT NULL,
    descripcion    VARCHAR(255) NOT NULL,
    estado         VARCHAR(20)  NOT NULL DEFAULT 'Pendiente',
    CONSTRAINT fk_cita_cliente
    FOREIGN KEY (id_cliente)
    REFERENCES cliente(id_cliente)
    ON UPDATE CASCADE
    ON DELETE RESTRICT,
    CONSTRAINT chk_cita_estado
    CHECK (estado IN ('Pendiente', 'En proceso', 'Finalizada', 'Cancelada')),
    CONSTRAINT chk_cita_tipo_servicio
    CHECK (length(trim(tipo_servicio)) > 0),
    CONSTRAINT chk_cita_descripcion
    CHECK (length(trim(descripcion)) > 0)
    );

-- ---------------------------------------------------------------------
-- Índices de apoyo (búsquedas y filtros)
-- ---------------------------------------------------------------------
-- Búsqueda de clientes por nombre/apellido (sin distinguir mayúsculas)
CREATE INDEX IF NOT EXISTS idx_cliente_nombres   ON cliente (LOWER(nombres));
CREATE INDEX IF NOT EXISTS idx_cliente_apellidos ON cliente (LOWER(apellidos));

-- Filtros de citas
CREATE INDEX IF NOT EXISTS idx_cita_fecha   ON cita_tecnica (fecha);
CREATE INDEX IF NOT EXISTS idx_cita_estado  ON cita_tecnica (estado);
CREATE INDEX IF NOT EXISTS idx_cita_cliente ON cita_tecnica (id_cliente);

-- ---------------------------------------------------------------------
-- Datos de prueba
-- ---------------------------------------------------------------------

-- 1) Clientes
INSERT INTO cliente (nombres, apellidos, telefono, correo, activo) VALUES
                                                                       ('Carlos',  'Rodríguez', '8888-1111', 'carlos.rodriguez@uam.edu.ni', TRUE),
                                                                       ('Kenneth', 'Zamora',    '8888-2222', 'kenneth.zamora@uam.edu.ni',  TRUE),
                                                                       ('Steven',  'Cuadra',    '8888-3333', 'steven.cuadra@uam.edu.ni',   TRUE),
                                                                       ('Kate',    'Ramírez',   '8888-4444', 'kate.ramirez@uam.edu.ni',    TRUE)
    ON CONFLICT (correo) DO NOTHING;

-- 2) Citas
--    No se asume ningún id_cliente fijo: se resuelve por correo.
--    Se inserta sólo si no existe ya la misma cita (mismo cliente + fecha + hora).

-- Cita para Carlos Rodríguez: mantenimiento preventivo hoy 09:00
INSERT INTO cita_tecnica
(id_cliente, fecha, hora, tipo_servicio, descripcion, estado)
SELECT c.id_cliente, CURRENT_DATE, '09:00',
       'Mantenimiento preventivo',
       'Limpieza interna y cambio de pasta térmica',
       'Pendiente'
FROM cliente c
WHERE c.correo = 'carlos.rodriguez@uam.edu.ni'
  AND NOT EXISTS (
    SELECT 1 FROM cita_tecnica ct
    WHERE ct.id_cliente = c.id_cliente
      AND ct.fecha = CURRENT_DATE
      AND ct.hora  = '09:00'
);

-- Cita para Carlos Rodríguez: reparación de hardware mañana 11:30
INSERT INTO cita_tecnica
(id_cliente, fecha, hora, tipo_servicio, descripcion, estado)
SELECT c.id_cliente, CURRENT_DATE + 1, '11:30',
       'Reparación de hardware',
       'Reemplazo de disco duro',
       'En proceso'
FROM cliente c
WHERE c.correo = 'carlos.rodriguez@uam.edu.ni'
  AND NOT EXISTS (
    SELECT 1 FROM cita_tecnica ct
    WHERE ct.id_cliente = c.id_cliente
      AND ct.fecha = CURRENT_DATE + 1
      AND ct.hora  = '11:30'
);

-- Cita para Kenneth Zamora: instalación de software hoy 14:00
INSERT INTO cita_tecnica
(id_cliente, fecha, hora, tipo_servicio, descripcion, estado)
SELECT c.id_cliente, CURRENT_DATE, '14:00',
       'Instalación de software',
       'Instalación de Office y antivirus',
       'Finalizada'
FROM cliente c
WHERE c.correo = 'kenneth.zamora@uam.edu.ni'
  AND NOT EXISTS (
    SELECT 1 FROM cita_tecnica ct
    WHERE ct.id_cliente = c.id_cliente
      AND ct.fecha = CURRENT_DATE
      AND ct.hora  = '14:00'
);

-- Cita para Steven Cuadra: diagnóstico en 2 días 10:15
INSERT INTO cita_tecnica
(id_cliente, fecha, hora, tipo_servicio, descripcion, estado)
SELECT c.id_cliente, CURRENT_DATE + 2, '10:15',
       'Diagnóstico',
       'Equipo no enciende',
       'Pendiente'
FROM cliente c
WHERE c.correo = 'steven.cuadra@uam.edu.ni'
  AND NOT EXISTS (
    SELECT 1 FROM cita_tecnica ct
    WHERE ct.id_cliente = c.id_cliente
      AND ct.fecha = CURRENT_DATE + 2
      AND ct.hora  = '10:15'
);

-- Cita para Kate Ramírez: reparación de hardware ayer 16:45
INSERT INTO cita_tecnica
(id_cliente, fecha, hora, tipo_servicio, descripcion, estado)
SELECT c.id_cliente, CURRENT_DATE - 1, '16:45',
       'Reparación de hardware',
       'Cambio de teclado',
       'Cancelada'
FROM cliente c
WHERE c.correo = 'kate.ramirez@uam.edu.ni'
  AND NOT EXISTS (
    SELECT 1 FROM cita_tecnica ct
    WHERE ct.id_cliente = c.id_cliente
      AND ct.fecha = CURRENT_DATE - 1
      AND ct.hora  = '16:45'
);

-- =====================================================================
-- OPCIONAL: migración para instalaciones que YA tienen las tablas
-- creadas con la versión anterior del script.
-- Descomentar y ejecutar sólo si es necesario.
-- =====================================================================
-- ALTER TABLE cliente
--     ALTER COLUMN telefono SET NOT NULL;
--
-- ALTER TABLE cliente
--     ADD CONSTRAINT chk_cliente_telefono CHECK (length(trim(telefono)) > 0);
--
-- ALTER TABLE cita_tecnica
--     ALTER COLUMN descripcion TYPE VARCHAR(255),
--     ALTER COLUMN descripcion SET NOT NULL;
--
-- ALTER TABLE cita_tecnica
--     ADD CONSTRAINT chk_cita_descripcion CHECK (length(trim(descripcion)) > 0);
--
-- CREATE UNIQUE INDEX IF NOT EXISTS uq_cliente_correo_lower
--     ON cliente (LOWER(correo));