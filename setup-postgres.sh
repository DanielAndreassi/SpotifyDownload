#!/bin/bash

echo "Configurando banco PostgreSQL para Spotify Downloader..."

# Verificar se PostgreSQL está instalado
if ! command -v psql &> /dev/null; then
    echo "PostgreSQL não está instalado. Instalando..."
    sudo apt update
    sudo apt install -y postgresql postgresql-contrib
fi

# Iniciar serviço PostgreSQL
sudo systemctl start postgresql
sudo systemctl enable postgresql

# Criar usuário e banco de dados
sudo -u postgres psql << EOF
-- Criar usuário
CREATE USER spotify_user WITH PASSWORD 'spotify_password';

-- Criar banco de dados
CREATE DATABASE spotify_downloader_db OWNER spotify_user;

-- Conceder privilégios
GRANT ALL PRIVILEGES ON DATABASE spotify_downloader_db TO spotify_user;

-- Conectar ao banco e conceder privilégios no schema public
\c spotify_downloader_db;
GRANT ALL ON SCHEMA public TO spotify_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO spotify_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO spotify_user;

\q
EOF

echo "Banco PostgreSQL configurado com sucesso!"
echo "Database: spotify_downloader_db"
echo "User: spotify_user"
echo "Password: spotify_password"