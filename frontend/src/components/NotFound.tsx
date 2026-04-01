import { Link } from "react-router-dom";

function NotFound() {
  return (
    <section className="animate-fade-in-up flex min-h-[70vh] items-center justify-center">
      <div className="w-full max-w-xl rounded-3xl border border-slate-700 bg-slate-900/60 p-8 text-center shadow-[0_20px_80px_rgba(2,6,23,0.35)] backdrop-blur-xl">
        <p className="text-xs font-semibold uppercase tracking-[0.18em] text-emerald-500/80">
          Error 404
        </p>
        <h1 className="mt-3 font-['Manrope'] text-4xl font-extrabold tracking-tight text-slate-100">
          Page not found
        </h1>
        <p className="mt-3 text-sm text-slate-400 sm:text-base">
          The page you are looking for does not exist or may have been moved.
        </p>
        <Link
          to="/"
          className="mt-6 inline-flex items-center justify-center rounded-xl bg-emerald-500 px-5 py-3 text-sm font-semibold text-slate-900 transition-all duration-300 hover:bg-emerald-400 hover:shadow-[0_0_20px_rgba(16,185,129,0.3)]"
        >
          Go back home
        </Link>
      </div>
    </section>
  );
}

export default NotFound;
