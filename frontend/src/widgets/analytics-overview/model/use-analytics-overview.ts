"use client";

import { useCallback, useState } from "react";
import { ProfileAnalysis } from "@/entities/analytics/model/profile-analysis";
import { StatsResponse } from "@/entities/analytics/model/types";
import { apiFetch } from "@/shared/api/http";

type State = {
  loading: boolean;
  error: string | null;
  stats: StatsResponse | null;
  analysis: ProfileAnalysis | null;
};

export function useAnalyticsOverview() {
  const [state, setState] = useState<State>({
    loading: false,
    error: null,
    stats: null,
    analysis: null
  });

  const load = useCallback(async function load() {
    setState((prev) => ({ ...prev, loading: true, error: null }));

    try {
      const [stats, analysis] = await Promise.all([
        apiFetch<StatsResponse>("/connections/stats?top=5"),
        apiFetch<ProfileAnalysis>("/connections/analysis")
      ]);

      setState({
        loading: false,
        error: null,
        stats,
        analysis
      });
    } catch (error) {
      setState({
        loading: false,
        error: error instanceof Error ? error.message : "Erro ao carregar analytics.",
        stats: null,
        analysis: null
      });
    }
  }, []);

  return {
    ...state,
    load
  };
}
