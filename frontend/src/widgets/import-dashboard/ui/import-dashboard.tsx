"use client";

import { useState } from "react";
import { ImportResult } from "@/entities/import/model/types";
import { UploadConnectionsForm } from "@/features/upload-connections/ui/upload-connections-form";
import { ImportResultView } from "@/features/view-import-result/ui/import-result-view";
import { Card } from "@/shared/ui/card";
import { AnalyticsOverview } from "@/widgets/analytics-overview/ui/analytics-overview";

export function ImportDashboard() {
  const [refreshToken, setRefreshToken] = useState(0);
  const [result, setResult] = useState<ImportResult | null>(null);

  return (
    <div className="grid gap-5 md:gap-6">
      <header className="grid gap-3 rounded-[26px] border border-app-border bg-app-surface px-6 py-8 text-center shadow-app-card md:px-12 md:py-10">
        <p className="m-0 justify-self-start text-[11px] font-semibold uppercase tracking-[0.18em] text-app-accent-strong md:text-[12px]">
          LinkedIn Review
        </p>
        <h1 className="m-0 text-[40px] font-medium leading-[1.08] md:text-[56px] lg:text-[72px]">
          Melhore seu perfil do Linkedin
        </h1>
        <p className="m-0 justify-self-center text-center max-w-[740px] text-sm leading-7 text-app-muted md:text-base">
          Envie o ZIP exportado do LinkedIn ou os CSVs relevantes e veja pontos de atenção sobre networking,
          conexões estratégicas, projetos e skills.
        </p>
      </header>

      <div className="grid items-start gap-5 xl:grid-cols-[minmax(300px,380px)_minmax(0,1fr)]">
        <div className="grid gap-5">
          <Card title="Upload">
            <UploadConnectionsForm onImported={() => setRefreshToken((prev) => prev + 1)} onResultChange={setResult} />
          </Card>

          <ImportResultView result={result} />
        </div>

        <AnalyticsOverview refreshToken={refreshToken} />
      </div>
    </div>
  );
}
