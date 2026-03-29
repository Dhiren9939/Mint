import logo from "../assets/logo.svg";

function Footer() {
  return (
    <footer className="mt-auto border-t border-slate-800 pt-6 pb-8 px-6">
      <div className="max-w-2xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-3 text-sm text-slate-500">
        <div className="flex items-center gap-2">
          <img src={logo} className="h-4 opacity-50" alt="Mint" />
          <span>
            Built by{" "}
            <a
              href="https://github.com/Dhiren9939"
              target="_blank"
              rel="noopener noreferrer"
              className="text-slate-400 hover:text-emerald-500 transition-colors"
            >
              Dhiren9939
            </a>
          </span>
        </div>
        <a
          href="https://github.com/Dhiren9939/Mint"
          target="_blank"
          rel="noopener noreferrer"
          className="text-slate-500 hover:text-emerald-500 transition-colors"
        >
          GitHub ↗
        </a>
      </div>
    </footer>
  );
}

export default Footer;
