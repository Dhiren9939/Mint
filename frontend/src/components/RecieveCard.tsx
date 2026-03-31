import { useState, type SubmitEvent } from "react";
import { Download } from "lucide-react";
import GlassCard from "./GlassCard";
import toast from "react-hot-toast";
import getDownloadLink, {
  type GetDownloadLinkResponse,
} from "../api/getDownloadLink";
import { AxiosError } from "axios";
import type { ApiResponse } from "../api";

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
    <div className="flex justify-center animate-fade-in-up stagger-2">
      <div className="max-w-lg w-full flex flex-col gap-4">
        <GlassCard>
          <form onSubmit={handleDownload}>
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
