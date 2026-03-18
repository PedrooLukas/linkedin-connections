import { ImportResult } from "@/entities/import/model/types";
import { Card } from "@/shared/ui/card";

export function ImportResultView({ result }: { result: ImportResult | null }) {
  if (!result) {
    return null;
  }

  return (
    <Card title="Resultado do Import">
      <div className="mb-3.5 grid grid-cols-2 gap-3">
        <div className="rounded-[14px] bg-app-surface-alt p-3.5">
          <p className="mb-1 text-[13px] text-app-muted">Importados</p>
          <p className="m-0 text-[38px] leading-none font-bold">{result.imported}</p>
        </div>
        <div className="rounded-[14px] bg-app-surface-alt p-3.5">
          <p className="mb-1 text-[13px] text-app-muted">Pulados</p>
          <p className="m-0 text-[38px] leading-none font-bold">{result.skipped}</p>
        </div>
      </div>

      {result.warnings.length > 0 ? (
        <ul className="m-0 list-disc pl-5 text-sm leading-6 text-app-muted">
          {result.warnings.map((warning, index) => (
            <li key={`${warning}-${index}`}>{warning}</li>
          ))}
        </ul>
      ) : (
        <p className="m-0 text-sm text-app-muted">Sem warnings.</p>
      )}
    </Card>
  );
}
