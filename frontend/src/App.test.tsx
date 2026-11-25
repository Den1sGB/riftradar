import { render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { App } from "./App";
import { AuthProvider } from "./context/AuthContext";
import { api } from "./api/client";

vi.mock("./api/client", async () => {
  const actual = await vi.importActual<typeof import("./api/client")>("./api/client");
  return {
    ...actual,
    api: {
      ...actual.api,
      lookupSummoner: vi.fn(),
    },
  };
});

describe("RiftRadar App", () => {
  beforeEach(() => {
    vi.mocked(api.lookupSummoner).mockResolvedValue({
      profile: {
        puuid: "mock-puuid",
        platform: "BR1",
        gameName: "Faker",
        tagLine: "KR1",
        summonerLevel: 420,
        profileIconId: 4512,
        soloTier: "DIAMOND",
        soloRank: "II",
        soloLp: 74,
        soloWins: 80,
        soloLosses: 60,
      },
      radar: {
        totalMatches: 10,
        wins: 7,
        winRatePct: 70,
        avgKda: 4.2,
        avgCsPerMin: 8.1,
        avgVisionScore: 26,
        currentLossStreak: 0,
        tiltScore: 10,
        tiltStatus: "CHILL",
        topChampions: [{ championName: "Ahri", games: 5, wins: 4, winRatePct: 80, avgKda: 5.1 }],
      },
      recentMatches: [
        {
          matchId: "BR1_1234",
          queueId: 420,
          championName: "Ahri",
          lane: "MIDDLE",
          win: true,
          kills: 9,
          deaths: 2,
          assists: 7,
          csTotal: 220,
          csPerMin: 7.9,
          durationSeconds: 1680,
          visionScore: 28,
          goldEarned: 14200,
          playedAt: "2026-11-25T16:00:00Z",
        },
      ],
      cacheHit: false,
      gatewayMode: "mock",
      fetchedAt: "2026-11-25T18:00:00Z",
    });
  });

  it("renders summoner radar and match history from API", async () => {
    render(
      <AuthProvider>
        <App />
      </AuthProvider>,
    );

    await waitFor(() => {
      expect(screen.getByText(/DIAMOND II/i)).toBeInTheDocument();
    });
    expect(screen.getByText(/Tilt 10\/100/i)).toBeInTheDocument();
    expect(screen.getByText(/70%/i)).toBeInTheDocument();
    expect(screen.getByText("Ahri")).toBeInTheDocument();
  });
});
