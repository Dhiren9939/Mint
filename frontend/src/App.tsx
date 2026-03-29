import { useState } from "react";
import Navbar from "./components/Navbar";
import Home from "./components/Home";
import About from "./components/About";
import Footer from "./components/Footer";

function App() {
  const [aboutOpen, setAboutOpen] = useState(false);

  return (
    <div className="min-h-screen font-['Inter'] flex flex-col">
      <div className="ambient-bg" />
      <Navbar onAboutClick={() => setAboutOpen(true)} />
      <div className="px-6 pb-12 flex-1">
        <Home />
      </div>
      <Footer />
      <About isOpen={aboutOpen} onClose={() => setAboutOpen(false)} />
    </div>
  );
}

export default App;

