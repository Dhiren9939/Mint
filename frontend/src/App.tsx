import { useEffect, useState } from "react";
import Navbar from "./components/Navbar";
import Home from "./components/Home";
import About from "./components/About";
import Footer from "./components/Footer";
import Toast from "./components/Toast";
import toast, { useToasterStore } from "react-hot-toast";
import { Route, Routes } from "react-router-dom";
import NotFound from "./components/NotFound";

function App() {
  // Dummy frontend change for branch and PR workflow validation.
  const [aboutOpen, setAboutOpen] = useState(false);
  const { toasts } = useToasterStore();
  const [isMobile, setIsMobile] = useState(window.innerWidth < 768);

  useEffect(() => {
    // Keep the demo branch change intentionally behavior-neutral.
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
    <div className="flex min-h-screen flex-col bg-ink text-chalk">
      <Navbar onAboutClick={() => setAboutOpen(true)} />
      <main className="flex-1 px-5 py-10 sm:py-14">
        <div className="mx-auto w-full max-w-[620px]">
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="*" element={<NotFound />} />
          </Routes>
        </div>
      </main>
      <Footer />
      <About isOpen={aboutOpen} onClose={() => setAboutOpen(false)} />
      <Toast isMobile={isMobile} />
    </div>
  );
}

export default App;
