import { Link } from "react-router-dom";

function NotFound() {
  return (
    <section className="animate-fade-up flex min-h-[60vh] items-center justify-center">
      <div className="w-full max-w-sm rounded-[4px] border border-line bg-panel p-8 text-center">
        <p className="text-sm text-mist">Error 404</p>
        <h1 className="mt-2 text-2xl font-semibold tracking-tight text-chalk">
          Page not found
        </h1>
        <p className="mt-2 text-sm leading-relaxed text-mist">
          The page you are looking for does not exist or may have been moved.
        </p>
        <Link
          to="/"
          className="mt-6 inline-flex items-center justify-center rounded-[4px] bg-mint px-5 py-3 text-sm font-semibold text-ink transition-colors duration-150 hover:bg-mint/90"
        >
          Go back home
        </Link>
      </div>
    </section>
  );
}

export default NotFound;
