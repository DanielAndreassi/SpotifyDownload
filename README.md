# 🎵 Spotify Downloader Web

## 📖 Sobre o Projeto

O **Spotify Downloader Web** é uma aplicação desenvolvida para facilitar o download de músicas do Spotify através de uma interface web intuitiva. O projeto utiliza a API oficial do Spotify para listar suas playlists e a partir delas baixar as músicas em formato MP3, facilitando a gravação em pendrives ou outros dispositivos de armazenamento.

Este projeto foi inspirado no [spotifydl](https://github.com/rbouteiller/spotifydl/tree/main) e desenvolvido com o objetivo de ir além, proporcionando uma experiência mais amigável e funcional.

### ✨ Funcionalidades

- 🔐 Autenticação com conta Spotify via OAuth2
- 📋 Listagem de playlists pessoais e colaborativas
- 🎧 Visualização detalhada de álbuns e faixas
- ⬇️ Download de músicas individuais ou playlists completas
- 📱 Interface web responsiva com React
- 🔄 Progresso de download em tempo real via WebSocket
- 🗄️ Armazenamento seguro com PostgreSQL

## 🏗️ Arquitetura do Projeto

O projeto é dividido em duas partes principais:

### Backend (Spring Boot)
- **Framework**: Spring Boot 3.2.0
- **Linguagem**: Java 17
- **Banco de Dados**: PostgreSQL
- **Autenticação**: Spring Security + JWT + OAuth2
- **API**: Spotify Web API Java SDK
- **Comunicação em Tempo Real**: WebSocket

### Frontend (React + Vite)
- **Framework**: React 19.1.1 com TypeScript
- **Build Tool**: Vite
- **UI Framework**: React Bootstrap
- **Estilização**: Bootstrap 5.3.8
- **Comunicação**: Axios + Socket.io

## 🛠️ Requisitos do Sistema

### Pré-requisitos Obrigatórios
- **Java 17** ou superior
- **Node.js 18** ou superior
- **PostgreSQL 12** ou superior
- **Maven 3.6** ou superior
- **NPM** ou **Yarn**

### Ferramentas Necessárias
- **ngrok** (para tunneling HTTP/HTTPS)
- Conta no **Spotify Developer Dashboard**

## 📋 Configuração Inicial

### 1. Clonando o Repositório
```bash
git clone https://github.com/seu-usuario/spotify-downloader-web.git
cd spotify-downloader-web
```

### 2. Configuração do Banco de Dados
Execute o script de configuração do PostgreSQL:
```bash
chmod +x setup-postgres.sh
./setup-postgres.sh
```

**Ou configure manualmente:**
```sql
-- Conectar como usuário postgres
sudo -u postgres psql

-- Criar banco e usuário
CREATE DATABASE spotify_downloader_db;
CREATE USER postgres WITH PASSWORD '1234';
GRANT ALL PRIVILEGES ON DATABASE spotify_downloader_db TO postgres;
```

### 3. Configuração do Spotify Developer

1. Acesse [Spotify Developer Dashboard](https://developer.spotify.com/dashboard)
2. Crie uma nova aplicação
3. Anote o **Client ID** e **Client Secret**
4. Configure as URLs de redirecionamento:
   - `http://localhost:8080/api/auth/callback`
   - `https://SEU_TUNNEL_NGROK.ngrok-free.app/api/auth/callback`

### 4. Configuração do ngrok
```bash
# Instalar ngrok (se não estiver instalado)
# Visite: https://ngrok.com/download

# Executar tunnel para o backend
ngrok http 8080

# Anote a URL gerada (ex: https://abc123.ngrok-free.app)
```

### 5. Configuração das Variáveis de Ambiente

Edite o arquivo `backend/src/main/resources/application.properties`:

```properties
# Spotify OAuth2 Configuration
spring.security.oauth2.client.registration.spotify.client-id=SEU_SPOTIFY_CLIENT_ID
spring.security.oauth2.client.registration.spotify.client-secret=SEU_SPOTIFY_CLIENT_SECRET

# URLs de redirecionamento (substitua pela sua URL do ngrok)
spotify.redirect-uri=https://SEU_TUNNEL_NGROK.ngrok-free.app/api/auth/callback
frontend.redirect-url=https://SEU_TUNNEL_NGROK.ngrok-free.app/callback

# CORS (adicione sua URL do ngrok)
cors.allowed-origins=http://localhost:3000,http://localhost:8080,http://localhost:5173,https://SEU_TUNNEL_NGROK.ngrok-free.app
```

## 🚀 Executando o Projeto

### Método Rápido (Recomendado)
```bash
chmod +x run-dev.sh
./run-dev.sh
```

### Método Manual

**Terminal 1 - Backend:**
```bash
cd backend
mvn clean install
mvn spring-boot:run
```

**Terminal 2 - Frontend:**
```bash
cd frontend
npm install
npm run dev
```

### Acessando a Aplicação
- **Frontend**: http://localhost:5173
- **Backend API**: http://localhost:8080/api
- **Documentação**: Consulte os endpoints no código fonte

## 🔧 Comandos Úteis

### Backend
```bash
# Compilar projeto
mvn clean compile

# Executar testes
mvn test

# Construir JAR
mvn clean package

# Executar em produção
java -jar target/spotify-downloader-backend-1.0.0.jar
```

### Frontend
```bash
# Instalar dependências
npm install

# Executar em desenvolvimento
npm run dev

# Construir para produção
npm run build

# Executar lint
npm run lint
```

### Banco de Dados
```bash
# Conectar ao banco
PGPASSWORD=1234 psql -h localhost -d spotify_downloader_db -U postgres

# Backup do banco
pg_dump -h localhost -U postgres spotify_downloader_db > backup.sql

# Restaurar backup
psql -h localhost -U postgres spotify_downloader_db < backup.sql
```

## 📁 Estrutura do Projeto

```
spotify-downloader-web/
├── backend/                    # Aplicação Spring Boot
│   ├── src/main/java/         # Código fonte Java
│   ├── src/main/resources/    # Recursos e configurações
│   ├── target/                # Arquivos compilados
│   └── pom.xml               # Configuração Maven
├── frontend/                  # Aplicação React
│   ├── src/                  # Código fonte TypeScript/React
│   ├── public/               # Arquivos estáticos
│   ├── dist/                 # Build de produção
│   └── package.json          # Configuração NPM
├── run-dev.sh               # Script de execução em desenvolvimento
├── setup-postgres.sh        # Script de configuração do banco
└── README.md                # Este arquivo
```

## 🔒 Segurança

- ✅ Autenticação OAuth2 com Spotify
- ✅ Tokens JWT para sessões
- ✅ Validação de entrada de dados
- ✅ CORS configurado adequadamente
- ✅ Senhas não armazenadas em texto plano

## 🐛 Problemas Conhecidos

- Algumas funcionalidades podem não estar 100% funcionais
- O projeto está sujeito a bugs não identificados
- Requer conhecimento básico em desenvolvimento para configuração
- Dependente de ferramentas externas (ngrok, Spotify API)

## 🤝 Contribuição

Este é um projeto de código aberto! Contribuições são bem-vindas:

1. Abra uma [Issue](https://github.com/seu-usuario/spotify-downloader-web/issues) para reportar bugs ou sugerir melhorias
2. Faça um Fork do projeto
3. Crie uma branch para sua feature (`git checkout -b feature/nova-funcionalidade`)
4. Commit suas mudanças (`git commit -m 'Adiciona nova funcionalidade'`)
5. Push para a branch (`git push origin feature/nova-funcionalidade`)
6. Abra um Pull Request

## 📄 Licença e Termos de Uso

**Desenvolvido por:** Daniel Andreassi Lopes

### ⚠️ Importante:
- Este projeto **NÃO PODE** ser comercializado
- Destinado apenas para uso pessoal e educacional
- Código aberto para fins de aprendizado e colaboração
- Respeite os termos de uso da API do Spotify
- Use apenas para download de conteúdo que você possui legalmente

## 📞 Suporte

Para dúvidas, problemas ou sugestões:
- Abra uma [Issue](https://github.com/seu-usuario/spotify-downloader-web/issues) no GitHub
- As atualizações não serão frequentes devido a compromissos acadêmicos e profissionais
- Manutenção será feita sempre que possível

## 🙏 Agradecimentos

- Projeto inspirado em [spotifydl](https://github.com/rbouteiller/spotifydl)
- Comunidade Spotify Developers
- Comunidade Spring Boot e React

---

**Nota:** Este projeto é para fins educacionais e de uso pessoal. Certifique-se de respeitar os direitos autorais e termos de serviço do Spotify.