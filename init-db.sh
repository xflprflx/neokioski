#!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# Script de inicialização do banco de dados.
# Executado automaticamente pelo PostgreSQL APENAS na primeira vez.
# ─────────────────────────────────────────────────────────────────────────────

set -e

echo "Criando usuário dedicado da aplicação: $APP_DB_USER"

psql -v ON_ERROR_STOP=1 --username "postgres" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER $APP_DB_USER WITH PASSWORD '$APP_DB_PASSWORD';
    GRANT CONNECT ON DATABASE $POSTGRES_DB TO $APP_DB_USER;
    GRANT USAGE, CREATE ON SCHEMA public TO $APP_DB_USER;
    GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO $APP_DB_USER;

    ALTER DEFAULT PRIVILEGES IN SCHEMA public
        GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO $APP_DB_USER;

    GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO $APP_DB_USER;

    ALTER DEFAULT PRIVILEGES IN SCHEMA public
        GRANT USAGE, SELECT ON SEQUENCES TO $APP_DB_USER;
EOSQL

echo "Usuário $APP_DB_USER criado com sucesso."