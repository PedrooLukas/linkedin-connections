import type { ReactNode } from "react";

export function Card({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="rounded-[20px] border border-app-border bg-app-surface p-4 shadow-app-card md:p-5">
      <h2 className="mb-3 text-[28px] font-semibold tracking-[0.01em] text-app-accent-strong">{title}</h2>
      {children}
    </section>
  );
}
