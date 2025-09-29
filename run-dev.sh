#!/bin/bash

# Função para encerrar os processos em segundo plano ao sair
cleanup() {
    echo "
Encerrando servidores..."
    kill $BACKEND_PID $FRONTEND_PID
    exit
}

# Captura o sinal de interrupção (Ctrl+C) e chama a função de limpeza
trap cleanup INT

# Inicia o backend
echo "Iniciando servidor backend..."
(cd backend && mvn spring-boot:run) &
BACKEND_PID=$!

# Aguarda um pouco para o backend iniciar antes do frontend
sleep 15

# Inicia o frontend
echo "Iniciando servidor frontend..."
(cd frontend && npm run dev) &
FRONTEND_PID=$!


echo ""
echo "=================================================="
echo "Servidores iniciados!"
echo "Backend rodando no PID: $BACKEND_PID"
echo "Frontend rodando no PID: $FRONTEND_PID"
echo ""
echo "Acesse a aplicação em: http://localhost:5173"
echo "Pressione Ctrl+C para encerrar ambos os servidores."
echo "=================================================="

# Aguarda os processos em segundo plano terminarem
wait
