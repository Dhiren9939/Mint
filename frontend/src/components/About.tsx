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
  { icon: <UserX size={16} />, label: "Fully anonymous" },
  { icon: <Shield size={16} />, label: "Secure S3 storage" },
  { icon: <Zap size={16} />, label: "Instant sharing" },
  { icon: <Clock size={16} />, label: "Auto-expiring links" },
  { icon: <Upload size={16} />, label: "Files & text" },
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
      <div className="absolute inset-0 bg-black/60" />

      <div
        className="relative z-10 w-full max-w-md rounded-[4px] border border-line bg-panel p-6"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="mb-5 flex items-center justify-between">
          <span className="text-lg font-semibold text-chalk">About Mint</span>
          <button
            onClick={onClose}
            className="cursor-pointer rounded-[4px] p-1.5 text-mist transition-colors duration-150 hover:text-chalk"
          >
            <X size={18} />
          </button>
        </div>

        <p className="mb-5 text-sm leading-relaxed text-mist">
          Mint is an anonymous file sharing platform. Upload files or text and
          get a shareable retrieval key — no sign-ups, no tracking. Files are
          hosted on S3 and auto-expire.
        </p>

        <div className="mb-5 flex flex-wrap gap-2">
          {features.map((f, i) => (
            <div
              key={i}
              className="flex items-center gap-1.5 rounded-[2px] border border-line px-2.5 py-1.5 text-xs text-mist"
            >
              <span className="text-mint">{f.icon}</span>
              {f.label}
            </div>
          ))}
        </div>

        <div className="mb-5 h-px bg-line" />

        <a
          href="https://github.com/Dhiren9939/Mint"
          target="_blank"
          rel="noopener noreferrer"
          className="group flex items-center gap-3 rounded-[4px] border border-line p-3 transition-colors duration-150 hover:border-mist"
        >
          <span className="text-mist group-hover:text-mint">
            <GithubIcon size={20} />
          </span>
          <div>
            <span className="text-sm font-medium text-chalk">
              Dhiren9939/Mint
            </span>
            <p className="text-xs text-mist">Source code on GitHub</p>
          </div>
        </a>

        <p className="mt-4 text-center text-xs text-mist">
          Built by Dhiren9939
        </p>
      </div>
    </div>
  );
}

export default About;
