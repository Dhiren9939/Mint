import { useEffect, useState } from "react";
import Navbar from "./components/Navbar";
import Home from "./components/Home";
import About from "./components/About";
import Footer from "./components/Footer";
import Toast from "./components/Toast";
import toast, { useToasterStore } from "react-hot-toast";

function App() {
  const [aboutOpen, setAboutOpen] = useState(false);
  const { toasts } = useToasterStore();
  const [isMobile, setIsMobile] = useState(window.innerWidth < 768);

  useEffect(() => {
    toasts
      .filter((t) => t.visible)
      .filter((_, i) => i >= 3)
      .forEach((t) => toast.dismiss(t.id));
  }, [toasts]);

  useEffect(() => {
    const handleResize = () => setIsMobile(window.innerWidth < 768);
    window.addEventListener("resize", handleResize);
    return () => window.removeEventListener("resize", handleResize);
  }, []);

  return (
    <div className="relative min-h-screen overflow-x-hidden font-['Inter'] text-slate-100">
      <div className="ambient-bg" />
      <div className="pointer-events-none absolute inset-x-0 top-0 h-56 bg-linear-to-b from-emerald-500/6 via-transparent to-transparent" />
      <div className="relative mx-auto flex min-h-screen w-full max-w-6xl flex-col px-4 sm:px-6 lg:px-10">
        <Navbar onAboutClick={() => setAboutOpen(true)} />
        <main className="flex-1 pb-12 pt-2 sm:pt-6">
          <Home />
        </main>
        <Footer />
      </div>
      <About isOpen={aboutOpen} onClose={() => setAboutOpen(false)} />
      <Toast isMobile={isMobile} />
    </div>
  );
}

export default App;
