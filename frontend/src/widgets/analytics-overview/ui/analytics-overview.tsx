"use client";

import { useEffect } from "react";
import { Button } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import { useAnalyticsOverview } from "@/widgets/analytics-overview/model/use-analytics-overview";

export function AnalyticsOverview({ refreshToken }: { refreshToken: number }) {
  const { loading, error, stats, analysis, load } = useAnalyticsOverview();

  useEffect(() => {
    if (refreshToken > 0) {
      void load();
    }
  }, [refreshToken, load]);

  return (
    <Card title="Analytics">
      <div className="mb-3 flex items-center justify-between gap-4">
        <p className="m-0 text-[15px] font-medium text-app-muted">Resumo apos o import.</p>
        <Button type="button" onClick={() => void load()} disabled={loading}>
          {loading ? "Carregando..." : "Atualizar"}
        </Button>
      </div>

      {error ? <p className="m-0 mb-3 text-sm text-app-danger">{error}</p> : null}

      {stats ? (
        <>
          <div className="mb-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            <div className="rounded-[14px] bg-app-surface-alt p-3.5">
              <p className="mb-1 text-[13px] text-app-muted">Conexoes</p>
              <p className="m-0 text-[42px] leading-none font-bold">{stats.totalConnections}</p>
            </div>
            <div className="rounded-[14px] bg-app-surface-alt p-3.5">
              <p className="mb-1 text-[13px] text-app-muted">Empresas mapeadas</p>
              <p className="m-0 text-[42px] leading-none font-bold">{stats.topCompanies.length}</p>
            </div>
            <div className="rounded-[14px] bg-app-surface-alt p-3.5">
              <p className="mb-1 text-[13px] text-app-muted">Cargos mapeados</p>
              <p className="m-0 text-[42px] leading-none font-bold">{stats.topPositions.length}</p>
            </div>
          </div>

          <div className="grid gap-3 md:grid-cols-2">
            <div className="rounded-[14px] bg-app-surface-alt p-3.5">
              <p className="mb-2 text-[22px] font-semibold">Top empresas</p>
              <ul className="m-0 list-disc pl-5 text-[15px] leading-7 text-app-muted">
                {stats.topCompanies.map((item) => (
                  <li key={`${item.value}-${item.total}`}>{item.value} ({item.total})</li>
                ))}
              </ul>
            </div>
            <div className="rounded-[14px] bg-app-surface-alt p-3.5">
              <p className="mb-2 text-[22px] font-semibold">Top cargos</p>
              <ul className="m-0 list-disc pl-5 text-[15px] leading-7 text-app-muted">
                {stats.topPositions.map((item) => (
                  <li key={`${item.value}-${item.total}`}>{item.value} ({item.total})</li>
                ))}
              </ul>
            </div>
          </div>
        </>
      ) : (
        <p className="m-0 text-sm text-app-muted">Nenhum dado carregado ainda.</p>
      )}

      {analysis ? (
        <div className="mt-4 rounded-[14px] border border-app-border bg-app-surface-alt p-4">
          <p className="mb-2 text-[26px] font-semibold">Analise do perfil</p>
          <p className="m-0 mb-1 text-[16px] leading-7 text-app-muted">{analysis.summary}</p>
          <p className="m-0 text-[15px] leading-7 text-app-muted">Total de conexoes: {analysis.totalConnections}</p>
          <p className="m-0 text-[15px] leading-7 text-app-muted">
            Conexoes estrategicas: {analysis.strategicConnections} ({analysis.strategicConnectionsRatio.toFixed(1)}%)
          </p>
          <p className="m-0 text-[15px] leading-7 text-app-muted">
            Projetos: {analysis.totalProjects} | Skills: {analysis.totalSkills}
          </p>
          <ul className="m-0 mt-1 list-disc pl-5 text-[15px] leading-7">
            {analysis.flags.map((flag) => (
              <li
                key={flag.code}
                className={
                  flag.severity === "danger"
                    ? "text-app-danger"
                    : flag.severity === "warning"
                      ? "text-app-warning"
                      : "text-app-success"
                }
              >
                <strong>{flag.title}:</strong> {flag.message}
              </li>
            ))}
          </ul>
        </div>
      ) : null}
    </Card>
  );
}
