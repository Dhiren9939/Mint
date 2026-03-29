import { useEffect } from "react";
import { Shield, Zap, UserX, Clock, Upload, X } from "lucide-react";

interface AboutProps {
  isOpen: boolean;
  onClose: () => void;
}

function GithubIcon({ size = 20 }: { size?: number }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M15 22v-4a4.8 4.8 0 0 0-1-3.5c3 0 6-2 6-5.5.08-1.25-.27-2.48-1-3.5.28-1.15.28-2.35 0-3.5 0 0-1 0-3 1.5-2.64-.5-5.36-.5-8 0C6 2 5 2 5 2c-.3 1.15-.3 2.35 0 3.5A5.403 5.403 0 0 0 4 9c0 3.5 3 5.5 6 5.5-.39.49-.68 1.05-.85 1.65-.17.6-.22 1.23-.15 1.85v4" />
      <path d="M9 18c-4.51 2-5-2-7-2" />
    </svg>
  );
}

const features = [
  { icon: <UserX size={18} />, label: "Fully Anonymous" },
  { icon: <Shield size={18} />, label: "Secure S3 Storage" },
  { icon: <Zap size={18} />, label: "Instant Sharing" },
  { icon: <Clock size={18} />, label: "Auto-Expiring Links" },
  { icon: <Upload size={18} />, label: "Files & Text" },
];

function About({ isOpen, onClose }: AboutProps) {
  useEffect(() => {
    if (!isOpen) return;
    document.body.style.overflow = "hidden";
    const handleKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", handleKey);
    return () => {
      document.body.style.overflow = "";
      window.removeEventListener("keydown", handleKey);
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
      onClick={onClose}
    >
      <div className="absolute inset-0 bg-black/80 backdrop-blur-md animate-fade-in" />

      <div
        className="relative z-10 w-full max-w-md animate-fade-in-up"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="border border-slate-700 rounded-2xl bg-linear-to-tr from-slate-700/30 to-emerald-300/10 shadow-2xs">
          <div className="rounded-2xl backdrop-blur-2xl py-6 px-6">
            <div className="flex items-center justify-between mb-5">
              <div className="flex items-center gap-2">
                <span className="text-emerald-500 text-xl font-bold font-['Manrope']">
                  About Mint
                </span>
              </div>
              <button
                onClick={onClose}
                className="p-1.5 rounded-lg text-slate-500 hover:text-slate-300 hover:bg-slate-700/50 transition-all duration-200 cursor-pointer"
              >
                <X size={18} />
              </button>
            </div>

            <p className="text-slate-400 text-sm leading-relaxed mb-5">
              Mint is an anonymous file sharing platform. Upload files or text
              and get a shareable retrieval key no sign-ups, no tracking. 
              Files are hosted on S3 and auto-expire.
            </p>

            <div className="flex flex-wrap gap-2 mb-5">
              {features.map((f, i) => (
                <div
                  key={i}
                  className="flex items-center gap-1.5 text-xs text-slate-300 bg-slate-700/30 border border-slate-700/50 rounded-lg px-3 py-1.5"
                >
                  <span className="text-emerald-500">{f.icon}</span>
                  {f.label}
                </div>
              ))}
            </div>

            <div className="h-px bg-slate-700/50 mb-5" />

            <a
              href="https://github.com/Dhiren9939/Mint"
              target="_blank"
              rel="noopener noreferrer"
              className="group flex items-center justify-between p-3 rounded-xl bg-slate-700/20 border border-slate-700/50 hover:border-emerald-500/30 transition-all duration-300"
            >
              <div className="flex items-center gap-3">
                <span className="text-slate-400 group-hover:text-emerald-500 transition-colors duration-300">
                  <GithubIcon size={20} />
                </span>
                <div>
                  <span className="text-sm font-medium text-slate-300 group-hover:text-emerald-400 transition-colors duration-300">
                    Dhiren9939/Mint
                  </span>
                  <p className="text-xs text-slate-500">Source code on GitHub</p>
                </div>
              </div>
              <span className="text-slate-600 group-hover:text-emerald-500 group-hover:translate-x-0.5 transition-all duration-300">
                →
              </span>
            </a>

            <p className="text-center text-xs text-slate-600 mt-4">
              Built by Dhiren9939
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

export default About;
