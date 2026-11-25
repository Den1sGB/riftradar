# Threat Model — RiftRadar

Modelo de ameaças enxuto (STRIDE) cobrindo riscos de **código, infraestrutura e negócio**.

## 1. Ativos Críticos
- **`RIOT_API_KEY`**: chave externa sujeita a revogação e rate limits rígidos da Riot Games.
- **`JWT_SECRET` & `refresh_tokens`**: controle de sessão e identidade dos usuários.
- **Cota da Riot API & Recursos de Banco/Cache**: disponibilidade do serviço contra abuso.
- **Dados do Usuário (`users`, `watchlist_entries`, `match_notes`)**: privacidade e isolamento por conta.

## 2. Ameaças e Mitigações Implementadas

| Categoria | Ameaça | Mitigação no RiftRadar |
| :--- | :--- | :--- |
| **Information Disclosure** | Vazamento da `RIOT_API_KEY` no frontend ou Git | Chave fica apenas no backend via env var (`HttpRiotGateway`); `.env` no `.gitignore`; header `X-Riot-Token` nunca exposto em logs ou respostas. |
| **Denial of Service / Cota** | Bot esgotar cota da Riot API ou derrubar login | `RateLimitFilter` com script Lua no Redis por IP e escopo (`/auth`, `/summoners`, `/api`), cache Redis + PostgreSQL e cooldown de 30s em `?refresh=true`. |
| **Broken Access Control (IDOR)** | Usuário A ler, editar ou deletar Watchlist/Notas do Usuário B | Todas as queries de escrita/leitura filtram por `(id, userId)` extraído do `Jwt.subject` (`WatchlistService`). |
| **Session Hijacking / Replay** | Reuso de Refresh Token roubado | Refresh tokens são armazenados apenas como hash SHA-256 (`refresh_tokens.token_hash`). Ao detectar reuso de token já revogado, `AuthService.refresh` revoga **todas** as sessões ativas do usuário. |
| **SSRF / Path Injection** | Manipular `{platform}` na URL para redirecionar chamadas Riot | `RiotRegion.fromPlatform()` valida contra uma allowlist estrita de regiões antes de montar o host da Riot API. |
| **Privilege Escalation em Container** | Container comprometido rodar como `root` | Ambos os `Dockerfile` rodam com usuário não-privilegiado (`riftradar` / `nginx-unprivileged`). |
| **Observability Exposure** | Expor `/actuator/metrics` ou `/actuator/env` na internet | `SecurityConfig` libera apenas `/actuator/health`; demais endpoints de actuator exigem `ROLE_ADMIN`. |
