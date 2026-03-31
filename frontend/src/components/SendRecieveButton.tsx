interface SendReceiveButtonProps {
  isSend: boolean;
  setIsSend: (value: boolean) => void;
}

function SendRecieveButton({ isSend, setIsSend }: SendReceiveButtonProps) {
  return (
    <aside className="lg:sticky lg:top-24">
      <div className="border border-slate-700/70 bg-slate-900/40 p-4 shadow-lg backdrop-blur-xl rounded-2xl">
        <p className="mb-3 text-xs font-semibold uppercase tracking-[0.15em] text-slate-500">
          Mode
        </p>
        <div className="relative flex w-full bg-slate-800/80 p-2 rounded-2xl border border-slate-700 shadow-2xs">
        <div
          className="absolute top-2 bottom-2 w-[calc(50%-8px)] bg-emerald-500 rounded-xl transition-transform duration-300 ease-in-out shadow-lg"
          style={{ transform: isSend ? "translateX(0)" : "translateX(100%)" }}
        />

        <button
          onClick={() => setIsSend(true)}
          className={`relative z-10 w-1/2 py-2 font-medium text-center transition-colors duration-300 rounded-xl cursor-pointer ${
            isSend ? "text-slate-800" : "text-slate-400 hover:text-slate-200"
          }`}
        >
          Send
        </button>
        <button
          onClick={() => setIsSend(false)}
          className={`relative z-10 w-1/2 py-2 font-medium text-center transition-colors duration-300 rounded-xl cursor-pointer ${
            !isSend ? "text-slate-800" : "text-slate-400 hover:text-slate-200"
          }`}
        >
          Recieve
        </button>
        </div>
        <p className="mt-3 text-xs text-slate-500">
          {isSend
            ? "Upload and share a temporary code."
            : "Enter a code to retrieve shared files."}
        </p>
      </div>
    </aside>
  );
}

export default SendRecieveButton;
