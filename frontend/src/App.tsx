import { useCallback, useEffect, useState } from "react";
import {
  Activity,
  BookmarkPlus,
  LogOut,
  NotebookPen,
  RefreshCw,
  Search,
  ShieldCheck,
  Trash2,
} from "lucide-react";
import { NavLink, Route, Routes, useNavigate } from "react-router-dom";
import { api, ApiClientError } from "./api/client";
import type { MatchNote, MoodTag, SummonerDashboard, WatchlistEntry } from "./api/types";
import { useAuth } from "./context/AuthContext";

const PLATFORMS = ["BR1", "NA1", "EUW1", "EUN1", "KR", "LA1", "LA2", "JP1"];
const REFRESH_COOLDOWN_SECONDS = 30;

export function App() {
  const { user, isAuthenticated, login, register, logout } = useAuth();
  const navigate = useNavigate();

  const [platform, setPlatform] = useState("BR1");
  const [riotIdInput, setRiotIdInput] = useState("Faker#KR1");
  const [dashboard, setDashboard] = useState<SummonerDashboard | null>(null);
  const [loadingSearch, setLoadingSearch] = useState(false);
  const [refreshCooldown, setRefreshCooldown] = useState(0);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const [watchlist, setWatchlist] = useState<WatchlistEntry[]>([]);
  const [notes, setNotes] = useState<MatchNote[]>([]);
  const [authMode, setAuthMode] = useState<"login" | "register">("login");
  const [email, setEmail] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [password, setPassword] = useState("");
  const [authError, setAuthError] = useState<string | null>(null);

  const [selectedMatchId, setSelectedMatchId] = useState<string | null>(null);
  const [moodTag, setMoodTag] = useState<MoodTag>("FOCUSED");
  const [noteText, setNoteText] = useState("");

  useEffect(() => {
    if (refreshCooldown <= 0) return;
    const timer = window.setInterval(() => {
      setRefreshCooldown((prev) => (prev <= 1 ? 0 : prev - 1));
    }, 1000);
    return () => window.clearInterval(timer);
  }, [refreshCooldown]);

  const searchSummoner = useCallback(
    async (targetPlatform: string, rawRiotId: string, forceRefresh = false) => {
      const [rawGame, rawTag] = rawRiotId.split("#");
      const gameName = rawGame?.trim();
      const tagLine = rawTag?.trim() || targetPlatform;
      if (!gameName) {
        setErrorMsg("Informe o Riot ID no formato Nome#TAG (ex.: Faker#KR1).");
        return;
      }

      setLoadingSearch(true);
      setErrorMsg(null);
      try {
        const result = await api.lookupSummoner(targetPlatform, gameName, tagLine, forceRefresh);
        setDashboard(result);
        if (forceRefresh) {
          setRefreshCooldown(REFRESH_COOLDOWN_SECONDS);
        }
      } catch (err) {
        if (err instanceof ApiClientError) {
          if (err.errorCode === "refresh_cooldown_active" || err.status === 429) {
            setRefreshCooldown((prev) => Math.max(prev, err.retryAfterSeconds ?? REFRESH_COOLDOWN_SECONDS));
          }
          setErrorMsg(err.message);
        } else {
          setErrorMsg("Falha ao buscar invocador.");
        }
      } finally {
        setLoadingSearch(false);
      }
    },
    [],
  );

  const loadProtectedData = useCallback(async () => {
    if (!isAuthenticated) {
      setWatchlist([]);
      setNotes([]);
      return;
    }
    try {
      const [wl, nt] = await Promise.all([api.listWatchlist(), api.listNotes()]);
      setWatchlist(wl);
      setNotes(nt);
    } catch {
      // Errors handled via AuthContext / inline notices
    }
  }, [isAuthenticated]);

  useEffect(() => {
    void searchSummoner("BR1", "Faker#KR1", false);
  }, [searchSummoner]);

  useEffect(() => {
    void loadProtectedData();
  }, [loadProtectedData]);

  const handleAuthSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setAuthError(null);
    try {
      if (authMode === "login") {
        await login(email, password);
      } else {
        await register(email, displayName, password);
      }
      setPassword("");
    } catch (err) {
      setAuthError(err instanceof ApiClientError ? err.message : "Falha na autenticação.");
    }
  };

  const handleAddWatchlist = async () => {
    if (!dashboard) return;
    try {
      const created = await api.addWatchlist(
        dashboard.profile.platform,
        dashboard.profile.gameName,
        dashboard.profile.tagLine,
      );
      setWatchlist((prev) => [created, ...prev]);
    } catch (err) {
      setErrorMsg(err instanceof ApiClientError ? err.message : "Não foi possível salvar na watchlist.");
    }
  };

  const handleRemoveWatchlist = async (id: string) => {
    await api.removeWatchlist(id);
    setWatchlist((prev) => prev.filter((item) => item.id !== id));
  };

  const handleCreateNote = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!dashboard || !selectedMatchId || !noteText.trim()) return;
    const created = await api.createNote(selectedMatchId, dashboard.profile.puuid, moodTag, noteText.trim());
    setNotes((prev) => [created, ...prev]);
    setNoteText("");
    setSelectedMatchId(null);
  };

  const handleDeleteNote = async (id: string) => {
    await api.deleteNote(id);
    setNotes((prev) => prev.filter((n) => n.id !== id));
  };

  const authOrWatchlistPanel = !isAuthenticated ? (
    <section className="card">
      <h3 style={{ marginTop: 0 }}>
        {authMode === "login" ? "Entrar no RiftRadar" : "Criar conta"}
      </h3>
      <p className="muted">
        Salve invocadores na Watchlist e registre notas táticas por partida.
      </p>
      <form className="stack" onSubmit={(e) => void handleAuthSubmit(e)}>
        <input
          type="email"
          required
          placeholder="seu@email.com"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        {authMode === "register" && (
          <input
            required
            minLength={2}
            placeholder="Nome de invocador"
            value={displayName}
            onChange={(e) => setDisplayName(e.target.value)}
          />
        )}
        <input
          type="password"
          required
          minLength={8}
          placeholder="Senha (mín. 8 caracteres)"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        {authError && <div className="alert-error">{authError}</div>}
        <button className="btn-primary" type="submit">
          {authMode === "login" ? "Entrar" : "Cadastrar"}
        </button>
      </form>
      <button
        className="btn-secondary"
        style={{ marginTop: 10, width: "100%", justifyContent: "center" }}
        onClick={() => setAuthMode(authMode === "login" ? "register" : "login")}
      >
        {authMode === "login" ? "Não tem conta? Cadastre-se" : "Já tenho conta"}
      </button>
    </section>
  ) : (
    <>
      <section className="card">
        <h3 style={{ marginTop: 0 }}>Minha Watchlist ({watchlist.length}/25)</h3>
        {watchlist.length === 0 ? (
          <p className="muted">Nenhum invocador salvo ainda.</p>
        ) : (
          <div className="stack">
            {watchlist.map((w) => (
              <div key={w.id} className="row-between stat-box">
                <div
                  style={{ cursor: "pointer" }}
                  onClick={() => {
                    const riotId = `${w.gameName}#${w.tagLine}`;
                    setPlatform(w.platform);
                    setRiotIdInput(riotId);
                    void searchSummoner(w.platform, riotId, false);
                    navigate("/");
                  }}
                >
                  <strong>
                    {w.gameName}#{w.tagLine}
                  </strong>
                  <div className="muted">
                    {w.platform} · {w.soloTier ?? "—"} {w.soloRank ?? ""} · Tilt {w.tiltScore ?? 0}
                  </div>
                </div>
                <button
                  className="btn-danger"
                  aria-label={`Remover ${w.gameName}`}
                  onClick={() => void handleRemoveWatchlist(w.id)}
                >
                  <Trash2 size={15} />
                </button>
              </div>
            ))}
          </div>
        )}
      </section>

      {selectedMatchId && (
        <section className="card">
          <h3 style={{ marginTop: 0 }}>Nova Nota ({selectedMatchId})</h3>
          <form className="stack" onSubmit={(e) => void handleCreateNote(e)}>
            <select value={moodTag} onChange={(e) => setMoodTag(e.target.value as MoodTag)}>
              <option value="FOCUSED">FOCUSED</option>
              <option value="CLUTCH">CLUTCH</option>
              <option value="REVIEW">REVIEW</option>
              <option value="TILTED">TILTED</option>
            </select>
            <textarea
              rows={3}
              required
              minLength={3}
              maxLength={500}
              placeholder="Ex: Errei controle de wave no nível 4 contra Zed."
              value={noteText}
              onChange={(e) => setNoteText(e.target.value)}
            />
            <div className="row-between">
              <button type="button" className="btn-secondary" onClick={() => setSelectedMatchId(null)}>
                Cancelar
              </button>
              <button type="submit" className="btn-primary">
                Salvar Nota
              </button>
            </div>
          </form>
        </section>
      )}

      <section className="card">
        <h3 style={{ marginTop: 0 }}>Notas Táticas ({notes.length})</h3>
        {notes.length === 0 ? (
          <p className="muted">Clique em "Nota" em uma partida para registrar aprendizados.</p>
        ) : (
          <div className="stack">
            {notes.map((n) => (
              <div key={n.id} className="stat-box">
                <div className="row-between">
                  <span className="badge">{n.moodTag}</span>
                  <button className="btn-danger" onClick={() => void handleDeleteNote(n.id)}>
                    <Trash2 size={14} />
                  </button>
                </div>
                <p style={{ margin: "8px 0 4px" }}>{n.noteText}</p>
                <div className="muted">{n.matchId}</div>
              </div>
            ))}
          </div>
        )}
      </section>
    </>
  );

  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="brand">
          <Activity size={24} color="#c89b3c" />
          <h1>RiftRadar</h1>
          <span className="badge">
            <ShieldCheck size={12} style={{ verticalAlign: "middle", marginRight: 4 }} />
            Spring Boot + Redis Guard
          </span>
        </div>

        <nav className="nav-tabs" aria-label="Navegação principal">
          <NavLink to="/" end className={({ isActive }) => `nav-link${isActive ? " active" : ""}`}>
            Radar &amp; Partidas
          </NavLink>
          <NavLink to="/watchlist" className={({ isActive }) => `nav-link${isActive ? " active" : ""}`}>
            Watchlist &amp; Notas{isAuthenticated ? ` (${watchlist.length})` : ""}
          </NavLink>
        </nav>

        <div className="nav-actions">
          {isAuthenticated && user ? (
            <>
              <span className="muted">
                Olá, <strong>{user.displayName}</strong>
              </span>
              <button className="btn-secondary" onClick={() => void logout()}>
                <LogOut size={16} /> Sair
              </button>
            </>
          ) : (
            <span className="muted">Modo leitura — entre para Watchlist e Notas</span>
          )}
        </div>
      </header>

      <Routes>
        <Route
          path="/"
          element={
            <>
              <section className="card">
                <form
                  className="search-bar"
                  onSubmit={(e) => {
                    e.preventDefault();
                    void searchSummoner(platform, riotIdInput, false);
                  }}
                >
                  <select aria-label="Região" value={platform} onChange={(e) => setPlatform(e.target.value)}>
                    {PLATFORMS.map((p) => (
                      <option key={p} value={p}>
                        {p}
                      </option>
                    ))}
                  </select>

                  <input
                    aria-label="Riot ID"
                    placeholder="Ex: Faker#KR1 ou brTT#BR1"
                    value={riotIdInput}
                    onChange={(e) => setRiotIdInput(e.target.value)}
                  />

                  <button className="btn-primary" type="submit" disabled={loadingSearch}>
                    <Search size={16} /> {loadingSearch ? "Buscando..." : "Analisar"}
                  </button>

                  <button
                    className="btn-secondary"
                    type="button"
                    disabled={loadingSearch || refreshCooldown > 0}
                    title={refreshCooldown > 0 ? `Aguarde ${refreshCooldown}s para atualizar novamente` : "Forçar atualização na Riot API"}
                    onClick={() => void searchSummoner(platform, riotIdInput, true)}
                  >
                    <RefreshCw size={16} />
                    {refreshCooldown > 0 ? `Aguarde ${refreshCooldown}s` : "Atualizar"}
                  </button>
                </form>

                <div className="quick-chips">
                  <span className="muted">Atalhos:</span>
                  {["Faker#KR1", "brTT#BR1", "ShowMaker#KR1"].map((sample) => (
                    <button
                      key={sample}
                      type="button"
                      className="chip"
                      onClick={() => {
                        setRiotIdInput(sample);
                        void searchSummoner(platform, sample, false);
                      }}
                    >
                      {sample}
                    </button>
                  ))}
                </div>

                {errorMsg && <div className="alert-error">{errorMsg}</div>}
              </section>

              <div className="grid-main">
                <main className="stack">
                  {dashboard && (
                    <>
                      <section className="card">
                        <div className="row-between">
                          <div style={{ display: "flex", alignItems: "center", gap: 14 }}>
                            <img
                              src={dashboard.profile.profileIconUrl}
                              alt={`${dashboard.profile.gameName} icon`}
                              width={56}
                              height={56}
                              style={{ borderRadius: "50%", border: "2px solid var(--gold)", flexShrink: 0 }}
                              onError={(e) => { (e.target as HTMLImageElement).style.display = "none"; }}
                            />
                            <div>
                              <h2 style={{ margin: 0 }}>
                                {dashboard.profile.gameName}
                                <span className="muted"> #{dashboard.profile.tagLine}</span>
                              </h2>
                              <div className="muted" style={{ marginTop: 4 }}>
                                {dashboard.profile.platform} · Nível {dashboard.profile.summonerLevel} ·{" "}
                                <strong>
                                  {dashboard.profile.soloTier} {dashboard.profile.soloRank}
                                </strong>{" "}
                                ({dashboard.profile.soloLp} LP) · {dashboard.cacheHit ? "Cache Redis/DB" : "Sync API"} (
                                {dashboard.gatewayMode}) · ddragon {dashboard.dataDragonVersion}
                              </div>
                            </div>
                          </div>

                          <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                            <span className={`tilt-pill tilt-${dashboard.radar.tiltStatus}`}>
                              Tilt {dashboard.radar.tiltScore}/100 · {dashboard.radar.tiltStatus}
                            </span>
                            {isAuthenticated && (
                              <button className="btn-secondary" onClick={() => void handleAddWatchlist()}>
                                <BookmarkPlus size={16} /> Watchlist
                              </button>
                            )}
                          </div>
                        </div>

                        <div className="stats-grid">
                          <div className="stat-box">
                            <div className="stat-label">Winrate ({dashboard.radar.totalMatches} jogos)</div>
                            <div className="stat-value">{dashboard.radar.winRatePct}%</div>
                          </div>
                          <div className="stat-box">
                            <div className="stat-label">KDA Médio</div>
                            <div className="stat-value">{dashboard.radar.avgKda}</div>
                          </div>
                          <div className="stat-box">
                            <div className="stat-label">CS / min</div>
                            <div className="stat-value">{dashboard.radar.avgCsPerMin}</div>
                          </div>
                          <div className="stat-box">
                            <div className="stat-label">Derrotas seguidas</div>
                            <div className="stat-value">{dashboard.radar.currentLossStreak}</div>
                          </div>
                        </div>

                        {dashboard.radar.topChampions.length > 0 && (
                          <div style={{ marginTop: 14 }}>
                            <div className="stat-label" style={{ marginBottom: 6 }}>
                              Campeões mais jogados
                            </div>
                            <div style={{ display: "flex", flexWrap: "wrap", gap: 8 }}>
                              {dashboard.radar.topChampions.map((c) => (
                                <span key={c.championName} className="badge">
                                  {c.championName}: {c.games}J · {c.winRatePct}% WR · {c.avgKda} KDA
                                </span>
                              ))}
                            </div>
                          </div>
                        )}
                      </section>

                      <section className="card">
                        <h3 style={{ marginTop: 0 }}>Histórico Recente</h3>
                        <div className="match-list">
                          {dashboard.recentMatches.map((m) => (
                            <div key={m.matchId} className={`match-row ${m.win ? "win" : "loss"}`}>
                              <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                                <img
                                  src={m.championIconUrl}
                                  alt={m.championName}
                                  width={36}
                                  height={36}
                                  style={{ borderRadius: 6, border: "1px solid var(--border)", flexShrink: 0 }}
                                  onError={(e) => { (e.target as HTMLImageElement).style.display = "none"; }}
                                />
                                <div>
                                  <strong>{m.championName}</strong>
                                  <div className="muted">{m.lane}</div>
                                </div>
                              </div>
                              <div>
                                <strong>
                                  {m.kills}/{m.deaths}/{m.assists}
                                </strong>{" "}
                                <span className="muted">· {m.csPerMin} CS/m · Visão {m.visionScore}</span>
                              </div>
                              <div className="muted">
                                {m.win ? "Vitória" : "Derrota"} · {Math.round(m.durationSeconds / 60)}m
                              </div>
                              <div>
                                {isAuthenticated && (
                                  <button
                                    className="btn-secondary"
                                    onClick={() => setSelectedMatchId(m.matchId)}
                                    title="Anotar aprendizado pós-jogo"
                                  >
                                    <NotebookPen size={15} /> Nota
                                  </button>
                                )}
                              </div>
                            </div>
                          ))}
                        </div>
                      </section>
                    </>
                  )}
                </main>

                <aside className="stack">{authOrWatchlistPanel}</aside>
              </div>
            </>
          }
        />

        <Route
          path="/watchlist"
          element={
            <div className="stack" style={{ maxWidth: 680 }}>
              {authOrWatchlistPanel}
            </div>
          }
        />
      </Routes>
    </div>
  );
}
