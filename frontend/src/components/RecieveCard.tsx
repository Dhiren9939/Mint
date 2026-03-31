import { useState } from "react";
import { Download } from "lucide-react";
import GlassCard from "./GlassCard";
import ErrorBanner from "./ErrorBanner";

function RecieveCard() {
  const [fileCode, setFileCode] = useState("");
  const [error, setError] = useState<string | null>(null);

  return (
    <div className="flex justify-center animate-fade-in-up stagger-2">
      <div className="max-w-lg w-full flex flex-col gap-4">
        <ErrorBanner error={error} />
        <GlassCard>
          <form>
            <div className="flex flex-col gap-2">
              <div className="text-md text-slate-300/80 font-medium">
                <label htmlFor="fileCode">RETRIEVAL KEY</label>
              </div>
              <div>
                <input
                  className="focus:outline-none focus:ring-1 focus:ring-emerald-500/30 text-3xl leading-10 font-semibold text-center bg-slate-700/15 rounded-lg w-full py-6 placeholder:text-slate-500 focus:placeholder-transparent text-slate-200 tracking-[0.25rem] transition-shadow duration-300"
                  type="text"
                  name="fileCode"
                  id="fileCode"
                  placeholder="0a9z2x"
                  maxLength={6}
                  spellCheck={false}
                  autoComplete="off"
                  onChange={(e) => setFileCode(e.target.value)}
                  value={fileCode}
                ></input>
              </div>
              <button
                type="submit"
                disabled={!fileCode.trim()}
                className="font-semibold rounded-lg flex w-full justify-center gap-2 bg-emerald-500 py-5 mt-4 text-slate-900 cursor-pointer hover:bg-emerald-400 hover:shadow-[0_0_20px_rgba(16,185,129,0.3)] disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:shadow-none disabled:active:scale-100 active:scale-[0.98] transition-all duration-300"
              >
                <Download stroke="#1e293b" /> Download Files
              </button>
            </div>
          </form>
        </GlassCard>
      </div>
    </div>
  );
}

export default RecieveCard;
