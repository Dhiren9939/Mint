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
    <div className="min-h-screen font-['Inter'] flex flex-col">
      <div className="ambient-bg" />
      <Navbar onAboutClick={() => setAboutOpen(true)} />
      <div className="px-6 pb-12 flex-1">
        <Home />
      </div>
      <Footer />
      <About isOpen={aboutOpen} onClose={() => setAboutOpen(false)} />
      <Toast isMobile={isMobile} />
    </div>
  );
}

export default App;
