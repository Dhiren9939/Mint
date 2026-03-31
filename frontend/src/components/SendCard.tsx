import { useState, type ChangeEvent } from "react";
import {
  Clock,
  DownloadCloud,
  File as FileIcon,
  Upload,
  Pencil,
  ChevronLeft,
  CornerDownLeft,
  Copy,
} from "lucide-react";
import GlassCard from "./GlassCard";
import Dropzone from "react-dropzone";
import { type ExpiryDuration } from "../api";
import getUploadLink from "../api/getUploadLink";
import toast from "react-hot-toast";
import uploadFile from "../api/uploadFile";
import confirmUpload from "../api/confirmUpload";
import { AxiosError } from "axios";

interface ExpiryOptionProps {
  duration: string;
  activeIndex: number;
  index: number;
  handleOptionClick: (index: number) => void;
}

function ExpiryOption({
  duration,
  activeIndex,
  index,
  handleOptionClick,
}: ExpiryOptionProps) {
  return (
    <button
      onClick={() => handleOptionClick(index)}
      className={`${
        activeIndex === index
          ? "bg-emerald-500 text-slate-800 shadow-md scale-105"
          : "bg-slate-700/30 text-slate-300 hover:bg-slate-700/60 hover:text-emerald-400"
      } p-2 w-14 text-center rounded-md cursor-pointer transition-all duration-200 ease-in-out`}
    >
      {duration}
    </button>
  );
}

function SendContent() {
  function checkAndReturn(
    val: string | null,
    lowerBound: number,
    upperBound: number,
    def: number,
  ): number {
    const num = Number(val ?? def);
    if (num < lowerBound || num > upperBound) return def;
    return num;
  }

  const localDownloadCount = checkAndReturn(
    localStorage.getItem("downloadCount"),
    1,
    100,
    100,
  );
  const localActiveIndex = checkAndReturn(
    localStorage.getItem("activeIndex"),
    0,
    3,
    2,
  );

  const [downloadCount, setDownloadCount] =
    useState<number>(localDownloadCount);
  const [activeIndex, setActiveIndex] = useState<number>(localActiveIndex);
  const activeIndexToExpiry: ExpiryDuration[] = [
    "MINUTES15",
    "MINUTES30",
    "MINUTES60",
    "HOURS24",
  ];

  const [file, setFile] = useState<File>();

  const [isTextMode, setIsTextMode] = useState<boolean>(false);
  const [textContent, setTextContent] = useState<string>("");
  const [fileCode, setFileCode] = useState<string>("");

  function convertTextToFile() {
    if (!textContent.trim()) return;
    const blob = new Blob([textContent], { type: "text/plain" });
    const newFile = new File([blob], "text-snippet.txt", {
      type: "text/plain",
    });
    setFile(newFile);
    setTextContent("");
    setIsTextMode(false);
    setFileCode("");
  }

  function handleSlider(e: ChangeEvent<HTMLInputElement>) {
    setDownloadCount(Number(e.target.value));
    localStorage.setItem("downloadCount", e.target.value);
  }
  function handleOptionClick(index: number) {
    setActiveIndex(index);
    localStorage.setItem("activeIndex", String(index));
  }

  function handleUnfocus(e: ChangeEvent<HTMLInputElement>) {
    if (Number(e.target.value) < 1) {
      setDownloadCount(100);
      localStorage.setItem("downloadCount", e.target.value);
    }
  }

  function handleInputChange(e: ChangeEvent<HTMLInputElement>) {
    const newValue = e.target.value;
    if (newValue === "" || /^\d+$/.test(newValue)) {
      const numValue = newValue === "" ? 0 : Number(newValue);
      if (numValue <= 100 && numValue >= 0) {
        setDownloadCount(numValue);
        localStorage.setItem("downloadCount", String(numValue));
      }
    }
  }

  async function handleUpload() {
    if (!file) {
      toast.error("Please select a file to upload.");
      return;
    }

    const expiryDuration: ExpiryDuration = activeIndexToExpiry[activeIndex];
    const maxDownload = downloadCount;

    const toastId = toast.loading("Uploading file...");
    try {
      const uploadLinkRes = await getUploadLink(
        file,
        expiryDuration,
        maxDownload,
      );
      if (!uploadLinkRes.data.data) throw new Error("Failed to upload file.");

      const {
        fileKey,
        fileCode: newFileCode,
        fileUrl,
      } = uploadLinkRes.data.data;

      await uploadFile(file, fileUrl);

      const confirmUploadRes = await confirmUpload(fileKey, newFileCode);
      if (!confirmUploadRes.data.data)
        throw new Error("Failed to upload file.");

      setFileCode(newFileCode);

      toast.success("Upload successful.", { id: toastId });
    } catch (error) {
      if (!(error instanceof AxiosError) || !error.response) {
        toast.error(
          error instanceof Error ? error.message : "Failed to upload file.",
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
        case "THIS_SHOULD_HAVE_BEEN_IMPOSSIBLE":
          toast.error("Could not generate file code. HOW????");
          break;
        case undefined:
          toast.error("Failed to upload file.", { id: toastId });
      }
    }
  }

  return (
    <div className="animate-fade-in-up">
      <div className="mx-auto flex w-full max-w-4xl flex-col gap-5 text-slate-300">
        <GlassCard>
          <div className="mb-5 flex items-center justify-between gap-3">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">
                Upload
              </p>
              <h2 className="font-['Manrope'] text-xl font-bold text-slate-100">
                {isTextMode ? "Share text as a file" : "Choose what to send"}
              </h2>
            </div>
            {!isTextMode && !file && (
              <button
                type="button"
                onClick={() => setIsTextMode(true)}
                className="rounded-xl border border-slate-700 bg-slate-800/40 px-3 py-2 text-xs font-medium text-emerald-400 transition-colors duration-200 hover:bg-slate-700/60"
              >
                Share text instead
              </button>
            )}
          </div>

          {isTextMode ? (
            <div className="flex min-h-[320px] flex-col gap-4 rounded-2xl border border-emerald-500/25 bg-slate-900/50 p-5">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2 text-emerald-500">
                  <Pencil size={18} />
                  <span className="text-sm font-semibold">Text Content</span>
                </div>
                <button
                  onClick={() => setIsTextMode(false)}
                  className="flex items-center gap-1 text-xs text-slate-500 transition-colors hover:text-slate-300"
                >
                  <ChevronLeft size={14} /> Back to file upload
                </button>
              </div>
              <textarea
                autoFocus
                className="custom-scrollbar min-h-[220px] w-full flex-1 resize-none rounded-xl border border-slate-700/70 bg-slate-900/40 p-4 text-base text-slate-200 outline-none transition-colors placeholder:text-slate-600 focus:border-emerald-500/40"
                placeholder="Type or paste your content here..."
                value={textContent}
                onChange={(e) => setTextContent(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter" && !e.shiftKey) {
                    e.preventDefault();
                    convertTextToFile();
                  }
                }}
              />

              <div className="mt-auto flex justify-end border-t border-slate-700/50 pt-3">
                <button
                  onClick={convertTextToFile}
                  disabled={!textContent.trim()}
                  className="cursor-pointer rounded-xl bg-emerald-500/10 px-4 py-2 text-sm font-medium text-emerald-400 transition-all duration-200 hover:bg-emerald-500/30 disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:bg-emerald-500/10"
                >
                  <span className="flex items-center gap-2">
                    <CornerDownLeft size={16} />
                    Convert to File
                  </span>
                </button>
              </div>
            </div>
          ) : (
            <Dropzone
              onDrop={(acceptedFiles) => {
                setFile(acceptedFiles[0]);
                setFileCode("");
              }}
              multiple={false}
            >
              {({ getRootProps, getInputProps, isDragActive }) => (
                <div
                  {...getRootProps()}
                  className={`group w-full cursor-pointer rounded-2xl border-2 border-dashed px-5 py-14 text-center transition-all duration-300 ${
                    isDragActive
                      ? "scale-[1.01] border-emerald-500 bg-emerald-500/10 shadow-[0_0_24px_rgba(16,185,129,0.18)]"
                      : "border-slate-700 bg-slate-800/30 hover:border-emerald-500/50 hover:bg-slate-800/60"
                  }`}
                >
                  <input {...getInputProps()} />

                  <div
                    className={`mx-auto mb-4 flex w-fit items-center justify-center rounded-2xl bg-slate-800 p-3 transition-transform duration-300 ${
                      isDragActive
                        ? "scale-110 shadow-[0_0_20px_rgba(16,185,129,0.2)]"
                        : "group-hover:scale-110"
                    }`}
                  >
                    {file ? (
                      <FileIcon stroke="#10b981" />
                    ) : (
                      <Upload stroke="#10b981" />
                    )}
                  </div>

                  <h2 className="text-xl font-semibold text-slate-200">
                    {isDragActive
                      ? "Release to drop"
                      : file
                        ? file.name
                        : "Drop your file here"}
                  </h2>

                  {!file ? (
                    <p className="mt-2 text-sm text-slate-400">
                      Drag and drop or{" "}
                      <span className="font-medium text-emerald-500">
                        browse your computer
                      </span>
                    </p>
                  ) : (
                    <p className="mt-2 text-sm text-slate-400">
                      File Size: {Math.ceil(file.size / 1024)} KB
                    </p>
                  )}

                  {file && (
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        setFile(undefined);
                        setFileCode("");
                      }}
                      className="mt-4 text-sm font-medium text-rose-400 transition-colors hover:text-rose-300 hover:underline"
                    >
                      Clear file
                    </button>
                  )}
                </div>
              )}
            </Dropzone>
          )}
        </GlassCard>

        <GlassCard>
          <div className="grid gap-6 sm:grid-cols-2">
            <div className="flex flex-col gap-3">
              <div className="flex items-center gap-2">
                <Clock stroke="#10b981" height={"1.25rem"} width={"1.25rem"} />
                <span className="text-sm font-semibold text-slate-300">
                  Expiry Duration
                </span>
              </div>
              <div className="flex flex-wrap items-center gap-2">
                {["15m", "30m", "1hr", "24hr"].map((time, idx) => (
                  <ExpiryOption
                    key={time}
                    duration={time}
                    index={idx}
                    activeIndex={activeIndex}
                    handleOptionClick={handleOptionClick}
                  />
                ))}
              </div>
            </div>
            <div className="flex flex-col gap-3">
              <div className="flex items-center gap-2">
                <DownloadCloud
                  stroke="#10b981"
                  height={"1.25rem"}
                  width={"1.25rem"}
                />
                <span className="text-sm font-semibold text-slate-300">
                  Download Limit
                </span>
              </div>
              <div className="flex items-center gap-2">
                <input
                  className="h-1.5 w-full cursor-pointer appearance-none rounded-lg bg-slate-700 accent-emerald-500"
                  type="range"
                  min={1}
                  max={100}
                  value={downloadCount}
                  onChange={handleSlider}
                ></input>
                <input
                  className="w-14 rounded-md bg-slate-700/30 p-2 text-center text-emerald-400 transition-colors duration-200 hover:bg-slate-700/50 focus:bg-slate-700/70 focus:outline-none"
                  type="numeric"
                  value={downloadCount}
                  onChange={handleInputChange}
                  onBlur={handleUnfocus}
                ></input>
              </div>
            </div>
          </div>

          <div className="mt-6 w-full">
            <button
              onClick={handleUpload}
              disabled={!file}
              className="flex w-full cursor-pointer justify-center gap-2 rounded-xl bg-emerald-500 py-4 font-semibold text-slate-900 transition-all duration-300 hover:bg-emerald-400 hover:shadow-[0_0_20px_rgba(16,185,129,0.3)] disabled:cursor-not-allowed disabled:opacity-50 active:scale-[0.99]"
            >
              <Upload />
              Upload File
            </button>
          </div>

          {fileCode && (
            <div className="animate-slide-down mt-5 flex w-full flex-col items-center gap-3 rounded-xl border border-emerald-500/30 bg-slate-900/50 p-4">
              <p className="text-sm text-slate-400">
                Your file is ready. Use this code to download it:
              </p>
              <div className="flex items-center gap-3 rounded-lg border border-slate-700 bg-slate-800 px-6 py-3">
                <span className="font-mono text-2xl font-bold tracking-widest text-emerald-400">
                  {fileCode}
                </span>
                <button
                  onClick={() => {
                    navigator.clipboard.writeText(fileCode);
                    toast.success("Code copied!");
                  }}
                  className="rounded-md p-2 text-slate-400 transition-colors hover:bg-slate-700/50 hover:text-emerald-400"
                  title="Copy code"
                >
                  <Copy size={20} />
                </button>
              </div>
            </div>
          )}
        </GlassCard>
      </div>
    </div>
  );
}

export default SendContent;
