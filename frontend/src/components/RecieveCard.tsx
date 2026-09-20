import { useState, type SubmitEvent } from "react";
import { Download } from "lucide-react";
import Panel from "./Panel";
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
    <div className="animate-fade-up">
      <Panel>
        <form onSubmit={handleDownload}>
          <div className="flex flex-col gap-5">
            <h2 className="text-lg font-semibold text-chalk">
              Enter your file code
            </h2>
            <div>
              <label htmlFor="fileCode" className="sr-only">
                File code
              </label>
              <input
                className="w-full rounded-[4px] border border-line bg-inset py-5 text-center font-mono text-2xl tracking-[0.3em] text-chalk placeholder:text-mist/50 focus:outline-none"
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
              className="flex w-full cursor-pointer items-center justify-center gap-2 rounded-[4px] bg-mint py-3.5 text-sm font-semibold text-ink transition-colors duration-150 hover:bg-mint/90 disabled:cursor-not-allowed disabled:bg-inset disabled:text-mist"
            >
              <Download size={17} /> Download File
            </button>
          </div>
        </form>
      </Panel>
    </div>
  );
}

export default RecieveCard;
