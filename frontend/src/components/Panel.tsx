import type React from "react";

interface PanelProps {
  children?: React.ReactNode;
}

function Panel({ children }: PanelProps) {
  return (
    <div className="rounded-[4px] border border-line bg-panel px-5 py-6 sm:px-7 sm:py-7">
      {children}
    </div>
  );
}

export default Panel;
