-- Inicialización de base de datos PostgreSQL para Zero Trust
-- Este archivo se ejecuta automáticamente cuando se crea el contenedor

-- Crear extensiones necesarias
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Configurar timezone
SET timezone = 'UTC';

-- Log inicial
DO $
BEGIN
    RAISE NOTICE 'Base de datos Zero Trust inicializada correctamente';
END $;
