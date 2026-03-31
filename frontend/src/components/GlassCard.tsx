import type React from "react";

interface GlassCardProps {
  children?: React.ReactNode;
}

function GlassCard({ children }: GlassCardProps) {
  return (
    <div className="relative overflow-hidden rounded-2xl border border-slate-700/80 bg-linear-to-tr from-slate-800/70 via-slate-800/45 to-emerald-500/8 shadow-xl">
      <div className="pointer-events-none absolute inset-0 bg-linear-to-b from-white/3 via-transparent to-transparent" />
      <div className="relative rounded-2xl px-5 py-6 backdrop-blur-2xl sm:px-7 sm:py-7">
        {children}
      </div>
    </div>
  );
}

export default GlassCard;
