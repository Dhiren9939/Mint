import { useState } from "react";
import SendReceiveButton from "./SendRecieveButton";
import RecieveCard from "./RecieveCard";
import SendCard from "./SendCard";

function Home() {
  const [isSend, setIsSend] = useState(true);

  return (
    <section className="animate-fade-in-up">
      <div className="mx-auto flex w-full max-w-5xl flex-col gap-8">
        <div className="space-y-3 text-center">
          <p className="text-xs font-semibold uppercase tracking-[0.18em] text-emerald-500/80">
            Private file sharing
          </p>
          <h1 className="font-['Manrope'] text-3xl font-extrabold tracking-tight text-slate-100 sm:text-4xl">
            Send files and text with temporary retrieval keys
          </h1>
          <p className="mx-auto max-w-2xl text-sm text-slate-400 sm:text-base">
            Keep it fast and anonymous. Upload once, share a code, and let
            links expire automatically.
          </p>
        </div>

        <div className="grid items-start gap-6 lg:grid-cols-[220px_1fr]">
          <SendReceiveButton isSend={isSend} setIsSend={setIsSend} />
          <div>{isSend ? <SendCard /> : <RecieveCard />}</div>
        </div>
      </div>
    </section>
  );
}

export default Home;
