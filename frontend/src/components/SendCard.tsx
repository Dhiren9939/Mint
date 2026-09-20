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
import Panel from "./Panel";
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
      className={`w-12 cursor-pointer rounded-[4px] py-1.5 text-center font-mono text-sm transition-colors duration-150 ${
        activeIndex === index
          ? "bg-mint text-ink"
          : "bg-inset text-mist hover:text-chalk"
      }`}
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
    <div className="animate-fade-up">
      <div className="flex flex-col gap-5">
        <Panel>
          <div className="mb-5 flex items-center justify-between gap-3">
            <h2 className="text-lg font-semibold text-chalk">
              {isTextMode ? "Share text as a file" : "Choose what to send"}
            </h2>
            {!isTextMode && !file && (
              <button
                type="button"
                onClick={() => setIsTextMode(true)}
                className="cursor-pointer rounded-[4px] border border-line px-3 py-1.5 text-xs text-mist transition-colors duration-150 hover:text-chalk"
              >
                Share text instead
              </button>
            )}
          </div>

          {isTextMode ? (
            <div className="flex min-h-[280px] flex-col gap-4 rounded-[4px] border border-line bg-inset p-5">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2 text-mint">
                  <Pencil size={16} />
                  <span className="text-sm font-medium">Text content</span>
                </div>
                <button
                  onClick={() => setIsTextMode(false)}
                  className="flex cursor-pointer items-center gap-1 text-xs text-mist transition-colors duration-150 hover:text-chalk"
                >
                  <ChevronLeft size={13} /> Back to file upload
                </button>
              </div>
              <textarea
                autoFocus
                className="custom-scrollbar min-h-[180px] w-full flex-1 resize-none rounded-[4px] border border-line bg-ink p-4 text-sm text-chalk outline-none placeholder:text-mist/60"
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

              <div className="mt-auto flex justify-end border-t border-line pt-3">
                <button
                  onClick={convertTextToFile}
                  disabled={!textContent.trim()}
                  className="flex cursor-pointer items-center gap-2 rounded-[4px] px-4 py-2 text-sm font-medium text-mint transition-colors duration-150 hover:text-chalk disabled:cursor-not-allowed disabled:text-mist"
                >
                  <CornerDownLeft size={15} />
                  Convert to file
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
                  className={`w-full cursor-pointer rounded-[4px] border border-dashed px-5 py-12 text-center transition-colors duration-150 ${
                    isDragActive
                      ? "border-mint bg-inset"
                      : "border-line bg-panel hover:border-mist"
                  }`}
                >
                  <input {...getInputProps()} />

                  <div className="mx-auto mb-3 flex w-fit items-center justify-center">
                    {file ? (
                      <FileIcon size={22} stroke="#00bc7d" />
                    ) : (
                      <Upload size={22} stroke="#00bc7d" />
                    )}
                  </div>

                  <h3 className="text-base font-medium text-chalk">
                    {isDragActive
                      ? "Release to drop"
                      : file
                        ? file.name
                        : "Drop a file here"}
                  </h3>

                  {!file ? (
                    <p className="mt-1.5 text-sm text-mist">
                      or <span className="text-mint">choose one</span>
                    </p>
                  ) : (
                    <p className="mt-1.5 font-mono text-sm text-mist">
                      {Math.ceil(file.size / 1024)} KB
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
                      className="mt-3 text-sm text-ember transition-colors duration-150 hover:text-chalk"
                    >
                      Clear file
                    </button>
                  )}
                </div>
              )}
            </Dropzone>
          )}
        </Panel>

        <Panel>
          <div className="flex flex-col gap-4">
            <div className="flex items-center justify-between gap-4">
              <div className="flex items-center gap-2 text-sm text-mist">
                <Clock size={16} stroke="#8b9e96" />
                Expires in
              </div>
              <div className="flex items-center gap-2">
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

            <div className="h-px bg-line" />

            <div className="flex items-center justify-between gap-4">
              <div className="flex items-center gap-2 text-sm text-mist">
                <DownloadCloud size={16} stroke="#8b9e96" />
                Download limit
              </div>
              <div className="flex items-center gap-3">
                <input
                  className="h-1 w-32 cursor-pointer appearance-none rounded-full bg-line accent-mint"
                  type="range"
                  min={1}
                  max={100}
                  value={downloadCount}
                  onChange={handleSlider}
                ></input>
                <input
                  className="w-12 rounded-[4px] bg-inset p-1.5 text-center font-mono text-sm text-chalk focus:outline-none"
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
              className="flex w-full cursor-pointer items-center justify-center gap-2 rounded-[4px] bg-mint py-3.5 text-sm font-semibold text-ink transition-colors duration-150 hover:bg-mint/90 disabled:cursor-not-allowed disabled:bg-inset disabled:text-mist"
            >
              <Upload size={17} />
              Upload file
            </button>
          </div>

          {fileCode && (
            <div
              aria-live="polite"
              className="mt-5 flex w-full flex-col items-center gap-2 rounded-[4px] border border-line bg-inset p-5 text-center"
            >
              <p className="text-sm text-mist">Your code</p>
              <div className="flex items-center gap-3">
                <span className="font-mono text-[2.5rem] leading-none tracking-[0.15em] text-mint">
                  {fileCode}
                </span>
                <button
                  onClick={() => {
                    navigator.clipboard.writeText(fileCode);
                    toast.success("Code copied!");
                  }}
                  className="cursor-pointer rounded-[4px] p-2 text-mist transition-colors duration-150 hover:text-chalk"
                  title="Copy code"
                >
                  <Copy size={18} />
                </button>
              </div>
              <p className="text-sm text-mist">
                Anyone with this code can download the file.
              </p>
            </div>
          )}
        </Panel>
      </div>
    </div>
  );
}

export default SendContent;
