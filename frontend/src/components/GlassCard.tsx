import type React from "react";

interface GlassCardProps {
  children?: React.ReactNode;
}

function GlassCard({ children }: GlassCardProps) {
  return (
    <div className="border border-slate-700 rounded-2xl bg-linear-to-tr from-slate-700/30 to-emerald-300/10 shadow-2xs">
      <div className="rounded-2xl backdrop-blur-2xl py-8 px-8">{children}</div>
    </div>
  );
}

export default GlassCard;
