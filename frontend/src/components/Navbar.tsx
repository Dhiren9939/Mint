import { Info } from "lucide-react";
import logo from "../assets/logo.svg";

interface NavbarProps {
  onAboutClick: () => void;
}

function Navbar({ onAboutClick }: NavbarProps) {
  return (
    <nav className="flex w-full items-center justify-between px-5 pb-6 pt-8 sm:pb-4 font-['Manrope'] animate-slide-down">
      <a href="/" className="group flex items-center gap-2 no-underline">
        <img
          src={logo}
          className="h-7 transition-transform duration-300 group-hover:rotate-[-8deg] group-hover:scale-110"
          alt="Mint logo"
        />
        <span className="text-3xl text-emerald-500 font-bold tracking-tight">
          Mint
        </span>
      </a>

      <button
        onClick={onAboutClick}
        className="flex items-center gap-1.5 px-4 py-2 rounded-xl text-sm font-medium text-slate-500 hover:text-slate-300 hover:bg-slate-800 transition-all duration-300 cursor-pointer"
      >
        <Info size={16} />
        <span>About</span>
      </button>
    </nav>
  );
}

export default Navbar;
