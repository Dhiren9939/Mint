import React, { useState, type ChangeEvent } from "react";
import {
  Clock,
  DownloadCloud,
  File,
  Upload,
  Pencil,
  ChevronLeft,
} from "lucide-react";
import GlassCard from "./GlassCard";
import Dropzone from "react-dropzone";

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
      className={`${activeIndex == index ? "bg-emerald-500 text-slate-800" : "bg-slate-700/10"} p-2 w-14 text-center rounded-md cursor-pointer transition-colors duration-200 ease-in-out`}
    >
      {duration}
    </button>
  );
}

function SendContent() {
  const [downloadCount, setDownloadCount] = useState(100);
  const [activeIndex, setActiveIndex] = useState(2);
  const [file, setFile] = useState<File>();

  const [isTextMode, setIsTextMode] = useState(false);
  const [textContent, setTextContent] = useState("");

  function handleSlider(e: ChangeEvent<HTMLInputElement>) {
    setDownloadCount(Number(e.target.value));
  }
  function handleOptionClick(index: number) {
    setActiveIndex(index);
  }

  function handleUnfocus(e: ChangeEvent<HTMLInputElement>) {
    if (Number(e.target.value) < 1) setDownloadCount(1);
  }

  function handleInputChange(e: ChangeEvent<HTMLInputElement>) {
    const newValue = e.target.value;
    if (newValue === "" || /^\d+$/.test(newValue)) {
      const numValue = newValue === "" ? 0 : Number(newValue);
      if (numValue <= 100 && numValue >= 0) setDownloadCount(numValue);
    }
  }

  return (
    <div>
      <div className="flex flex-col gap-4 items-center justify-center text-slate-300">
        <div className="w-full max-w-2xl">
          {isTextMode ? (
            <div className="flex flex-col gap-4 p-6 border-2 border-emerald-500/30 bg-slate-900/50 rounded-2xl min-h-[300px]">
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
              />
            </div>
          ) : (
            <Dropzone
              onDrop={(acceptedFiles) => setFile(acceptedFiles[0])}
              multiple={false}
            >
              {({ getRootProps, getInputProps, isDragActive }) => (
                <div
                  {...getRootProps()}
                  className={`flex flex-col gap-2 items-center border-2 border-dashed text-center px-4 py-16 rounded-2xl w-full transition-all duration-300 cursor-pointer group
                    ${isDragActive ? "border-emerald-500 bg-emerald-500/5 scale-[1.01]" : "border-slate-700 bg-transparent hover:border-slate-500"}`}
                >
                  <input {...getInputProps()} />

                  <div
                    className={`p-3 rounded-2xl bg-slate-800 flex items-center justify-center transition-transform duration-300 
                    ${isDragActive ? "scale-110 shadow-[0_0_20px_rgba(16,185,129,0.2)]" : "group-hover:scale-110"}`}
                  >
                    {file ? (
                      <File stroke="#10b981" />
                    ) : (
                      <Upload stroke="#10b981" />
                    )}
                  </div>

                  <h2 className="text-xl font-semibold text-slate-200">
                    {isDragActive
                      ? "Release to drop"
                      : file
                        ? file.name
                        : "Drop your files here"}
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
                        <div className="h-[1px] w-8 bg-slate-700"></div>
                        <span className="text-xs text-slate-600 uppercase font-bold">
                          or
                        </span>
                        <div className="h-[1px] w-8 bg-slate-700"></div>
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
                    <h3 className="text-slate-400">
                      File Size: {Math.ceil(file.size / 1024)} KB
                    </h3>
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
                  <ExpiryOption
                    duration="15m"
                    index={0}
                    activeIndex={activeIndex}
                    handleOptionClick={handleOptionClick}
                  />
                  <ExpiryOption
                    duration="30m"
                    index={1}
                    activeIndex={activeIndex}
                    handleOptionClick={handleOptionClick}
                  />
                  <ExpiryOption
                    duration="1hr"
                    index={2}
                    activeIndex={activeIndex}
                    handleOptionClick={handleOptionClick}
                  />
                  <ExpiryOption
                    duration="24hr"
                    index={3}
                    activeIndex={activeIndex}
                    handleOptionClick={handleOptionClick}
                  />
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
                    className="focus:outline-none bg-slate-700/30 p-2 w-12 text-center rounded-md text-emerald-400"
                    type="numeric"
                    value={downloadCount}
                    onChange={handleInputChange}
                    onBlur={handleUnfocus}
                  ></input>
                </div>
              </div>
            </div>

            <div className="w-full flex justify-center">
              <button
                disabled={!file && !textContent}
                className="w-full font-semibold rounded-lg flex justify-center gap-2 text-slate-800 bg-emerald-500 py-5 mt-6 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed transition-opacity"
              >
                <Upload />
                {isTextMode ? "Generate Text Link" : "Upload files"}
              </button>
            </div>
          </GlassCard>
        </div>
      </div>
    </div>
  );
}

export default SendContent;
