import { Info } from "lucide-react";
import logo from "../assets/logo.svg";

interface NavbarProps {
  onAboutClick: () => void;
}

function Navbar({ onAboutClick }: NavbarProps) {
  return (
    <nav className="border-b border-line">
      <div className="mx-auto flex w-full max-w-[620px] items-center justify-between px-5 py-5">
        <a href="/" className="flex items-center gap-2 no-underline">
          <img src={logo} className="h-5 w-5" alt="Mint logo" />
          <span className="text-lg font-semibold tracking-tight text-chalk">
            Mint
          </span>
        </a>

        <button
          onClick={onAboutClick}
          className="flex cursor-pointer items-center gap-1.5 rounded-[4px] px-2 py-1.5 text-sm text-mist transition-colors duration-150 hover:text-chalk"
        >
          <Info size={15} />
          <span>About</span>
        </button>
      </div>
    </nav>
  );
}

export default Navbar;
