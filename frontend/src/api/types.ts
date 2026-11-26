export interface UserProfile {
  id: string;
  email: string;
  displayName: string;
  role: string;
  createdAt: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInSeconds: number;
  user: UserProfile;
}

export interface ChampionStat {
  championName: string;
  games: number;
  wins: number;
  winRatePct: number;
  avgKda: number;
}

export interface RadarSummary {
  totalMatches: number;
  wins: number;
  winRatePct: number;
  avgKda: number;
  avgCsPerMin: number;
  avgVisionScore: number;
  currentLossStreak: number;
  tiltScore: number;
  tiltStatus: "CHILL" | "WARMING_UP" | "TILT_RISK" | "HIGH_TILT";
  topChampions: ChampionStat[];
}

export interface MatchItem {
  matchId: string;
  queueId: number;
  championName: string;
  championIconUrl: string;
  lane: string;
  win: boolean;
  kills: number;
  deaths: number;
  assists: number;
  csTotal: number;
  csPerMin: number;
  durationSeconds: number;
  visionScore: number;
  goldEarned: number;
  playedAt: string;
}

export interface SummonerProfile {
  puuid: string;
  platform: string;
  gameName: string;
  tagLine: string;
  summonerLevel: number;
  profileIconId: number;
  profileIconUrl: string;
  soloTier: string;
  soloRank: string;
  soloLp: number;
  soloWins: number;
  soloLosses: number;
}

export interface SummonerDashboard {
  profile: SummonerProfile;
  radar: RadarSummary;
  recentMatches: MatchItem[];
  cacheHit: boolean;
  gatewayMode: string;
  dataDragonVersion: string;
  dataDragonBaseUrl: string;
  fetchedAt: string;
}

export interface WatchlistEntry {
  id: string;
  platform: string;
  puuid: string;
  gameName: string;
  tagLine: string;
  aliasLabel: string | null;
  soloTier: string | null;
  soloRank: string | null;
  soloLp: number | null;
  tiltScore: number | null;
  createdAt: string;
}

export type MoodTag = "FOCUSED" | "CLUTCH" | "TILTED" | "REVIEW";

export interface MatchNote {
  id: string;
  matchId: string;
  puuid: string;
  moodTag: MoodTag;
  noteText: string;
  createdAt: string;
  updatedAt: string;
}

export interface ApiProblem {
  title: string;
  status: number;
  detail: string;
  errorCode?: string;
  retryAfterSeconds?: number;
}
