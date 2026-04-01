import logo from "../assets/logo.svg";

function Footer() {
  return (
    <footer className="mt-auto border-t border-slate-800/80 pb-8 pt-6">
      <div className="mx-auto flex max-w-5xl flex-col items-center justify-between gap-3 text-sm text-slate-500 sm:flex-row">
        <div className="flex items-center gap-2">
          <img src={logo} className="h-4 opacity-50" alt="Mint" />
          <span>
            Built by{" "}
            <a
              href="https://github.com/Dhiren9939"
              target="_blank"
              rel="noopener noreferrer"
              className="text-slate-400 transition-colors hover:text-emerald-500"
            >
              Dhiren9939
            </a>
          </span>
        </div>
        <a
          href="https://github.com/Dhiren9939/Mint"
          target="_blank"
          rel="noopener noreferrer"
          className="text-slate-500 transition-colors hover:text-emerald-500"
        >
          GitHub -&gt;
        </a>
      </div>
    </footer>
  );
}

export default Footer;
