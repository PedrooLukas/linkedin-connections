import { ButtonHTMLAttributes } from "react";

type Props = ButtonHTMLAttributes<HTMLButtonElement>;

export function Button({ className = "", disabled, ...props }: Props) {
  const baseClassName = [
    "inline-flex items-center justify-center rounded-xl border border-app-accent bg-app-accent px-5 py-2.5",
    "text-sm font-bold text-[#1a1a18] shadow-[0_10px_22px_rgba(245,143,105,0.28)] transition-all",
    disabled ? "cursor-not-allowed opacity-60 shadow-none" : "cursor-pointer hover:brightness-105",
    className
  ].join(" ");

  return <button disabled={disabled} className={baseClassName} {...props} />;
}
