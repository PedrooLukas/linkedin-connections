"use client";

import { ChangeEvent, useRef, useState } from "react";
import { useUploadConnections } from "@/features/upload-connections/model/use-upload-connections";

type Props = {
  onImported: (imported: number) => void;
  onResultChange: (result: { imported: number; skipped: number; warnings: string[] } | null) => void;
};

export function UploadConnectionsForm({ onImported, onResultChange }: Props) {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [selectedFilesLabel, setSelectedFilesLabel] = useState("Nenhum arquivo selecionado");
  const { loading, error, result, upload } = useUploadConnections();

  async function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
    const files = event.target.files;
    if (!files || files.length === 0) {
      setSelectedFilesLabel("Nenhum arquivo selecionado");
      return;
    }

    if (files.length === 1) {
      setSelectedFilesLabel(files[0].name);
    } else {
      setSelectedFilesLabel(`${files.length} arquivos selecionados`);
    }

    const uploadResult = await upload(files);
    onResultChange(uploadResult);
    if (uploadResult) {
      onImported(uploadResult.imported);
    }
  }

  return (
    <div className="grid gap-3.5">
      <div className="rounded-[14px] border border-app-border bg-app-surface-alt p-3.5">
        <p className="mb-1.5 text-[15px] font-semibold">Arquivos aceitos</p>
        <p className="m-0 text-[13px] leading-6 text-app-muted">
          ZIP do LinkedIn ou CSVs como `Connections`, `Projects`, `Skills` e `Comments`
        </p>
      </div>

      <div className="grid gap-2">
        <input
          ref={fileInputRef}
          id="connections-upload"
          type="file"
          multiple
          accept=".csv,.zip"
          onChange={handleFileChange}
          className="hidden"
        />

        <label
          htmlFor="connections-upload"
          className={[
            "flex w-full items-center justify-center rounded-xl border border-app-accent bg-app-accent px-4 py-3 text-[18px] font-bold text-[#1a1a18]",
            "shadow-[0_10px_24px_rgba(245,143,105,0.26)] transition-all",
            loading ? "cursor-not-allowed opacity-60 shadow-none" : "cursor-pointer hover:brightness-105"
          ].join(" ")}
        >
          {loading ? "Enviando..." : "Selecionar arquivo"}
        </label>

        <p className="m-0 break-all text-[15px] leading-6 text-app-muted">{selectedFilesLabel}</p>
      </div>

      {error ? <p className="m-0 text-[15px] text-app-danger">{error}</p> : null}

      {result ? (
        <p className="m-0 text-[15px] leading-6 text-app-muted">
          Importados: {result.imported} | Pulados: {result.skipped} | Warnings: {result.warnings.length}
        </p>
      ) : null}
    </div>
  );
}
