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
      <div className="flex flex-col gap-4 items-center justify-center text-slate-300">
        <div className="w-full max-w-2xl">
          {isTextMode ? (
            <div className="flex flex-col gap-4 p-6 border-2 border-emerald-500/30 bg-slate-900/50 rounded-2xl min-h-75">
              <div className="flex justify-between items-center">
                <div className="flex items-center gap-2 text-emerald-500">
                  <Pencil size={20} />
                  <span className="font-semibold">Text Content</span>
                </div>
                <button
                  onClick={() => setIsTextMode(false)}
                  className="text-xs text-slate-500 hover:text-slate-300 flex items-center gap-1 transition-colors"
                >
                  <ChevronLeft size={14} /> Back to file upload
                </button>
              </div>
              <textarea
                autoFocus
                className="w-full flex-1 bg-transparent border-none outline-none text-slate-200 resize-none text-lg placeholder:text-slate-600 custom-scrollbar"
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

              <div className="flex justify-end pt-2 border-t border-slate-700/50 mt-auto">
                <button
                  onClick={convertTextToFile}
                  disabled={!textContent.trim()}
                  className="flex items-center gap-2 px-4 py-2 bg-emerald-500/10 text-emerald-400 hover:bg-emerald-500/30 disabled:opacity-50 disabled:hover:bg-emerald-500/10 disabled:cursor-not-allowed rounded-xl text-sm font-medium transition-all duration-200 cursor-pointer"
                >
                  <CornerDownLeft size={16} />
                  Enter
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
                  className={`flex flex-col gap-2 items-center border-2 border-dashed text-center px-4 py-16 rounded-2xl w-full transition-all duration-300 cursor-pointer group
                    ${
                      isDragActive
                        ? "border-emerald-500 bg-emerald-500/10 scale-[1.02] shadow-[0_0_20px_rgba(16,185,129,0.15)]"
                        : "border-slate-700 bg-slate-800/40 hover:border-emerald-500/50 hover:bg-slate-800/60"
                    }`}
                >
                  <input {...getInputProps()} />

                  <div
                    className={`p-3 rounded-2xl bg-slate-800 flex items-center justify-center transition-transform duration-300 
                    ${isDragActive ? "scale-110 shadow-[0_0_20px_rgba(16,185,129,0.2)]" : "group-hover:scale-110"}`}
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
                    <div className="flex flex-col gap-3">
                      <h3 className="text-slate-400">
                        Or{" "}
                        <span className="text-emerald-500 font-medium">
                          browse your computer
                        </span>
                      </h3>
                      <div className="flex items-center gap-2 justify-center mt-2">
                        <div className="h-px w-8 bg-slate-700"></div>
                        <span className="text-xs text-slate-600 uppercase font-bold">
                          or
                        </span>
                        <div className="h-px w-8 bg-slate-700"></div>
                      </div>
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          setIsTextMode(true);
                        }}
                        className="text-emerald-500 text-sm hover:underline font-medium"
                      >
                        Share text instead
                      </button>
                    </div>
                  ) : (
                    <div className="flex flex-col items-center gap-3">
                      <h3 className="text-slate-400">
                        File Size: {Math.ceil(file.size / 1024)} KB
                      </h3>
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          setFile(undefined);
                          setFileCode("");
                        }}
                        className="text-rose-400 text-sm hover:text-rose-300 hover:underline font-medium transition-colors"
                      >
                        Clear file
                      </button>
                    </div>
                  )}
                </div>
              )}
            </Dropzone>
          )}
        </div>

        <div className="max-w-2xl w-full">
          <GlassCard>
            <div className="flex flex-col sm:flex-row gap-6 sm:gap-1">
              <div className="w-full flex flex-col gap-2">
                <div className="flex gap-2">
                  <Clock stroke="#10b981" height={"1.5rem"} width={"1.5rem"} />
                  <span>Expiry Duration</span>
                </div>
                <div className="flex items-center gap-2 h-full">
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
              <div className="w-full flex flex-col gap-2">
                <div className="flex gap-2">
                  <DownloadCloud
                    stroke="#10b981"
                    height={"1.5rem"}
                    width={"1.5rem"}
                  />
                  <span>Download Limit</span>
                </div>
                <div className="flex items-center gap-2">
                  <input
                    className="accent-emerald-500 bg-slate-700 rounded-lg h-1.5 appearance-none cursor-pointer w-full"
                    type="range"
                    min={1}
                    max={100}
                    value={downloadCount}
                    onChange={handleSlider}
                  ></input>
                  <input
                    className="focus:outline-none bg-slate-700/30 hover:bg-slate-700/50 focus:bg-slate-700/70 p-2 w-12 text-center rounded-md text-emerald-400 transition-colors duration-200"
                    type="numeric"
                    value={downloadCount}
                    onChange={handleInputChange}
                    onBlur={handleUnfocus}
                  ></input>
                </div>
              </div>
            </div>

            <div className="w-full flex flex-col items-center">
              <button
                onClick={handleUpload}
                disabled={!file}
                className="w-full font-semibold rounded-lg flex justify-center gap-2 text-slate-900 bg-emerald-500 py-5 mt-6 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-300 hover:bg-emerald-400 hover:shadow-[0_0_20px_rgba(16,185,129,0.3)] active:scale-[0.98]"
              >
                <Upload />
                Upload File
              </button>

              {fileCode && (
                <div className="w-full mt-6 bg-slate-900/50 border border-emerald-500/30 rounded-xl p-4 flex flex-col items-center gap-3 animate-slide-down">
                  <p className="text-slate-400 text-sm">
                    Your file is ready! Use this code to download it:
                  </p>
                  <div className="flex items-center gap-3 bg-slate-800 px-6 py-3 rounded-lg border border-slate-700">
                    <span className="font-mono text-2xl font-bold text-emerald-400 tracking-widest">
                      {fileCode}
                    </span>
                    <button
                      onClick={() => {
                        navigator.clipboard.writeText(fileCode);
                        toast.success("Code copied!");
                      }}
                      className="p-2 hover:bg-slate-700/50 text-slate-400 hover:text-emerald-400 rounded-md transition-colors"
                      title="Copy code"
                    >
                      <Copy size={20} />
                    </button>
                  </div>
                </div>
              )}
            </div>
          </GlassCard>
        </div>
      </div>
    </div>
  );
}

export default SendContent;
