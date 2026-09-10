\set ON_ERROR_STOP on

-- Execute conectado ao banco "postgres" como administrador. Substitua o marcador antes da primeira execução.
SELECT format('CREATE ROLE sismun_app WITH LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD %L', 'SUBSTITUIR_POR_SENHA_SEGURA')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'sismun_app') \gexec

-- CREATE DATABASE é emitido fora de bloco transacional.
SELECT 'CREATE DATABASE sismun OWNER sismun_app ENCODING ''UTF8'' TEMPLATE template0'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'sismun') \gexec

ALTER DATABASE sismun OWNER TO sismun_app;
GRANT CONNECT ON DATABASE sismun TO sismun_app;
\connect sismun
ALTER SCHEMA public OWNER TO sismun_app;
GRANT USAGE, CREATE ON SCHEMA public TO sismun_app;
