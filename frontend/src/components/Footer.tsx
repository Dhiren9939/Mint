function Footer() {
  return (
    <footer className="border-t border-line">
      <div className="mx-auto flex w-full max-w-[620px] flex-col items-center justify-between gap-2 px-5 py-6 text-sm text-mist sm:flex-row">
        <span>
          Built by{" "}
          <a
            href="https://github.com/Dhiren9939"
            target="_blank"
            rel="noopener noreferrer"
            className="text-mist transition-colors duration-150 hover:text-chalk"
          >
            Dhiren9939
          </a>
        </span>
        <a
          href="https://github.com/Dhiren9939/Mint"
          target="_blank"
          rel="noopener noreferrer"
          className="text-mist transition-colors duration-150 hover:text-chalk"
        >
          GitHub
        </a>
      </div>
    </footer>
  );
}

export default Footer;
