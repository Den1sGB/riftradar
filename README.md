# RiftRadar

Sistema **end-to-end** de analytics para **League of Legends**: busca de invocadores, histórico de partidas, cálculo de **Tilt Index**, Watchlist privada e notas táticas pós-jogo.

## Arquitetura & Stack

- **Backend:** Java 21 + Spring Boot 4.1 + Spring Security (JWT OAuth2 Resource Server) + Flyway + PostgreSQL 16 + Redis 7
- **Frontend:** React 19 + TypeScript + Vite + Vitest
- **Infraestrutura & Segurança:** Docker multi-stage (non-root), Docker Compose, Nginx headers CSP/XSS, Caddy TLS, GitHub Actions CI + Trivy, rate limiting distribuído via Lua no Redis, rotação de refresh token com detecção de replay e proteção anti-IDOR.

## Rodando Localmente

### Via Docker Compose (recomendado)
```bash
cp .env.example .env
docker compose up -d --build
```
Acesse `http://localhost:3000`.

### Rodando Testes Manualmente
- **Backend (unitários):**
  ```bash
  cd backend
  ./mvnw test
  ```
- **Backend (integração completa com PostgreSQL + Redis via Testcontainers — requer Docker ativo):**
  ```bash
  cd backend
  ./mvnw verify
  ```
- **Frontend:**
  ```bash
  cd frontend
  npm ci
  npm run typecheck
  npm test
  npm run build
  ```

## Documentação Adicional
- [Threat Model (Segurança do Negócio)](docs/threat-model.md)
- [Runbook Operacional](docs/runbook.md)
