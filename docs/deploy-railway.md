# Deploy no Railway — RiftRadar

Guia passo a passo para publicar o **RiftRadar** no Railway mantendo **custo mínimo** (\$5/mês de crédito gratuito no plano Hobby/Trial).

## Arquitetura no Railway
1. **PostgreSQL** (plugin oficial Railway)
2. **Redis** (plugin oficial Railway)
3. **Backend (`backend/`)** usando [railway.json](file:///c:/Users/denis/OneDrive/Desktop/RiftRadar/backend/railway.json) + `backend/Dockerfile`
4. **Frontend (`frontend/`)** — você pode hospedar gratuitamente na **Vercel / Cloudflare Pages** (custo \$0) apontando `VITE_API_BASE_URL` para a URL do backend no Railway, ou como serviço estático no próprio Railway.

## Passo a Passo (Backend + Bancos)

1. Acesse [railway.app](https://railway.app) e crie um **New Project** -> **Deploy from GitHub repo** -> selecione `Den1sGB/riftradar`.
2. Nas configurações do serviço do repo, defina **Root Directory** = `backend`.
3. No mesmo projeto, clique em **+ New** -> **Database** -> **Add PostgreSQL** e **Add Redis**.
4. Na aba **Variables** do serviço `backend`, configure:
   - `DB_URL`: `jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}`
   - `DB_USERNAME`: `${{Postgres.PGUSER}}`
   - `DB_PASSWORD`: `${{Postgres.PGPASSWORD}}`
   - `REDIS_HOST`: `${{Redis.REDISHOST}}`
   - `REDIS_PORT`: `${{Redis.REDISPORT}}`
   - `REDIS_PASSWORD`: `${{Redis.REDISPASSWORD}}`
   - `JWT_SECRET`: gere uma string aleatória de 64+ caracteres
   - `RIOT_GATEWAY_MODE`: `mock` (ou `http` se tiver `RIOT_API_KEY`)
   - `CORS_ALLOWED_ORIGINS`: URL final do frontend (ex.: `https://riftradar.vercel.app`)
5. Faça o deploy e valide `https://<seu-backend>.up.railway.app/actuator/health`.
