import type {
  ApiProblem,
  AuthResponse,
  MatchNote,
  MoodTag,
  SummonerDashboard,
  UserProfile,
  WatchlistEntry,
} from "./types";

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? "";

export class ApiClientError extends Error {
  readonly status: number;
  readonly errorCode?: string;

  constructor(problem: ApiProblem) {
    super(problem.detail || problem.title || "API request failed");
    this.status = problem.status;
    this.errorCode = problem.errorCode;
  }
}

interface TokenStore {
  getAccessToken: () => string | null;
  getRefreshToken: () => string | null;
  onTokensUpdated: (auth: AuthResponse) => void;
  onSessionExpired: () => void;
}

let tokenStore: TokenStore | null = null;

export function bindTokenStore(store: TokenStore) {
  tokenStore = store;
}

async function request<T>(path: string, init: RequestInit = {}, allowRefresh = true): Promise<T> {
  const headers = new Headers(init.headers);
  if (init.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  const accessToken = tokenStore?.getAccessToken();
  if (accessToken && !headers.has("Authorization")) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers,
  });

  if (response.status === 401 && allowRefresh && tokenStore?.getRefreshToken()) {
    const refreshed = await tryRefreshToken(tokenStore.getRefreshToken()!);
    if (refreshed) {
      return request<T>(path, init, false);
    }
    tokenStore.onSessionExpired();
  }

  if (!response.ok) {
    let problem: ApiProblem = {
      title: response.statusText,
      status: response.status,
      detail: `HTTP ${response.status}`,
    };
    try {
      problem = (await response.json()) as ApiProblem;
    } catch {
      // Ignore non-JSON error bodies
    }
    throw new ApiClientError(problem);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

async function tryRefreshToken(refreshToken: string): Promise<boolean> {
  try {
    const response = await fetch(`${API_BASE}/api/v1/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });
    if (!response.ok) {
      return false;
    }
    const auth = (await response.json()) as AuthResponse;
    tokenStore?.onTokensUpdated(auth);
    return true;
  } catch {
    return false;
  }
}

export const api = {
  register: (email: string, displayName: string, password: string) =>
    request<AuthResponse>("/api/v1/auth/register", {
      method: "POST",
      body: JSON.stringify({ email, displayName, password }),
    }, false),

  login: (email: string, password: string) =>
    request<AuthResponse>("/api/v1/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password }),
    }, false),

  logout: () =>
    request<void>("/api/v1/auth/logout", { method: "POST" }, false),

  me: () => request<UserProfile>("/api/v1/auth/me"),

  lookupSummoner: (platform: string, gameName: string, tagLine: string, refresh = false) =>
    request<SummonerDashboard>(
      `/api/v1/summoners/${encodeURIComponent(platform)}/${encodeURIComponent(gameName)}/${encodeURIComponent(tagLine)}?refresh=${refresh}`,
    ),

  listWatchlist: () => request<WatchlistEntry[]>("/api/v1/watchlist"),

  addWatchlist: (platform: string, gameName: string, tagLine: string, aliasLabel?: string) =>
    request<WatchlistEntry>("/api/v1/watchlist", {
      method: "POST",
      body: JSON.stringify({ platform, gameName, tagLine, aliasLabel }),
    }),

  removeWatchlist: (entryId: string) =>
    request<void>(`/api/v1/watchlist/${encodeURIComponent(entryId)}`, { method: "DELETE" }),

  listNotes: (matchId?: string) => {
    const query = matchId ? `?matchId=${encodeURIComponent(matchId)}` : "";
    return request<MatchNote[]>(`/api/v1/notes${query}`);
  },

  createNote: (matchId: string, puuid: string, moodTag: MoodTag, noteText: string) =>
    request<MatchNote>("/api/v1/notes", {
      method: "POST",
      body: JSON.stringify({ matchId, puuid, moodTag, noteText }),
    }),

  deleteNote: (noteId: string) =>
    request<void>(`/api/v1/notes/${encodeURIComponent(noteId)}`, { method: "DELETE" }),
};
