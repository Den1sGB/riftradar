# Runbook Operacional — RiftRadar

## 1. Subir Stack Completa em Docker

```bash
cp .env.example .env
# Edite .env com senhas fortes e JWT_SECRET aleatório (64+ caracteres)
docker compose up -d --build
```

- Frontend: `http://localhost:3000`
- Backend Health: `http://localhost:8080/actuator/health`
- Com Prometheus: `docker compose --profile observability up -d`

## 2. Alternar entre Modo Mock e Riot API Real

- **Modo estudo / offline / CI (`RIOT_GATEWAY_MODE=mock`):** gera invocadores e partidas determinísticas sem precisar de chave da Riot.
- **Modo produção/live (`RIOT_GATEWAY_MODE=http`):** gere uma Development API Key no Riot Developer Portal e atualize `RIOT_API_KEY` no `.env`:

```bash
docker compose up -d --no-deps backend
```

## 3. Backup e Restore do PostgreSQL

```bash
# Backup
docker compose exec -T postgres pg_dump -U riftradar riftradar > backup_$(date +%F).sql

# Restore
cat backup_2026-11-25.sql | docker compose exec -T postgres psql -U riftradar riftradar
```

## 4. Rotação de Segredos (`JWT_SECRET`)

1. Gere novo segredo (`openssl rand -hex 32`).
2. Atualize `JWT_SECRET` no `.env` e reinicie o `backend`.
3. Usuários reautenticarão automaticamente no próximo acesso expirado.
