import { useState, type SubmitEvent } from "react";
import { Download } from "lucide-react";
import GlassCard from "./GlassCard";
import toast from "react-hot-toast";
import getDownloadLink from "../api/getDownloadLink";
import { AxiosError } from "axios";

function RecieveCard() {
  const [fileCode, setFileCode] = useState("");

  async function handleDownload(e: SubmitEvent<HTMLFormElement>) {
    e.preventDefault();

    if (!/^[0-9a-z.]{6}/.test(fileCode)) {
      toast.error("Enter a valid file code.");
      return;
    }

    const toastId = toast.loading("Downloading file...");
    try {
      const downloadLinkRes = await getDownloadLink(fileCode);
      if (!downloadLinkRes.data.data) throw new Error("Download failed.");

      const downloadURL = downloadLinkRes.data.data.fileUrl;
      window.open(downloadURL);

      toast.success("File downloaded.", { id: toastId });
    } catch (error) {
      if (!(error instanceof AxiosError) || !error.response) {
        toast.error(
          error instanceof Error ? error.message : "Download failed.",
          { id: toastId },
        );
        return;
      }

      const errorCode = error.response.data?.error?.code;

      switch (errorCode) {
        case "RATE_LIMIT_EXCEEDED":
          toast.error("Too may requests.", { id: toastId });
          break;
        case "INVALID_FILE_CODE":
          toast.error("File not found.", { id: toastId });
          break;
        case undefined:
          toast.error("Download failed.", { id: toastId });
      }
    }
  }

  return (
    <div className="animate-fade-in-up stagger-2">
      <div className="w-full max-w-3xl">
        <GlassCard>
          <form onSubmit={handleDownload}>
            <div className="flex flex-col gap-4">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">
                  Download
                </p>
                <h2 className="font-['Manrope'] text-xl font-bold text-slate-100">
                  Enter your file code
                </h2>
              </div>
              <div>
                <label htmlFor="fileCode" className="sr-only">
                  File code
                </label>
                <input
                  className="w-full rounded-xl border border-slate-700/70 bg-slate-900/45 py-6 text-center text-3xl font-semibold leading-10 tracking-[0.25rem] text-slate-200 placeholder:text-slate-500 transition-shadow duration-300 focus:outline-none focus:ring-1 focus:ring-emerald-500/30 focus:placeholder-transparent"
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
                className="mt-2 flex w-full cursor-pointer justify-center gap-2 rounded-xl bg-emerald-500 py-4 font-semibold text-slate-900 transition-all duration-300 hover:bg-emerald-400 hover:shadow-[0_0_20px_rgba(16,185,129,0.3)] disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:shadow-none disabled:active:scale-100 active:scale-[0.98]"
              >
                <Download stroke="#1e293b" /> Download File
              </button>
            </div>
          </form>
        </GlassCard>
      </div>
    </div>
  );
}

export default RecieveCard;
