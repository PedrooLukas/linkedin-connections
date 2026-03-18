"use client";

import { useState } from "react";
import { ImportResult } from "@/entities/import/model/types";
import { API_BASE_URL } from "@/shared/config/env";

type UploadState = {
  loading: boolean;
  error: string | null;
  result: ImportResult | null;
};

export function useUploadConnections() {
  const [state, setState] = useState<UploadState>({
    loading: false,
    error: null,
    result: null
  });

  async function upload(files: FileList | null): Promise<ImportResult | null> {
    if (!files || files.length === 0) {
      setState((prev) => ({ ...prev, error: "Selecione ao menos um arquivo." }));
      return null;
    }

    const fileArray = Array.from(files);
    const formData = new FormData();

    const endpoint = fileArray.length === 1 ? "/connections/import" : "/connections/import/batch";
    const fieldName = fileArray.length === 1 ? "file" : "files";

    fileArray.forEach((file) => formData.append(fieldName, file));

    setState({ loading: true, error: null, result: null });

    try {
      const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        method: "POST",
        body: formData
      });

      if (!response.ok) {
        const text = await response.text();
        throw new Error(text || `Falha no upload (${response.status}).`);
      }

      const result = (await response.json()) as ImportResult;
      setState({ loading: false, error: null, result });
      return result;
    } catch (error) {
      setState({
        loading: false,
        error: error instanceof Error ? error.message : "Erro inesperado no upload.",
        result: null
      });
      return null;
    }
  }

  return {
    ...state,
    upload
  };
}
