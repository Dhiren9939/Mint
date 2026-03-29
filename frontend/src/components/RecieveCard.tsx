import React, { useState } from "react";
import { Download } from "lucide-react";
import GlassCard from "./GlassCard";

function RecieveCard() {
  const [fileCode, setFileCode] = useState("");

  return (
    <div className="flex justify-center">
      <div className="max-w-lg w-full">
        <GlassCard>
          <form>
            <div className="flex flex-col gap-2">
              <div className="text-md text-slate-300/80 font-medium">
                <label htmlFor="fileCode">RETRIEVAL KEY</label>
              </div>
              <div>
                <input
                  className="focus:outline-none text-3xl leading-10 font-semibold text-center bg-slate-700/15 rounded-lg w-full py-6 placeholder:text-slate-500 focus:placeholder-transparent text-slate-200 tracking-[0.25rem]"
                  type="text"
                  name="fileCode"
                  id="fileCode"
                  placeholder="0a9z2x"
                  maxLength={6}
                  onChange={(e) => setFileCode(e.target.value)}
                  value={fileCode}
                ></input>
              </div>
              <button
                type="submit"
                className="font-semibold rounded-lg flex w-full justify-center gap-2 bg-emerald-500 py-5 mt-4 text-slate-800 cursor-pointer"
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
