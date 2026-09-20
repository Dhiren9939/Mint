interface SendReceiveButtonProps {
  isSend: boolean;
  setIsSend: (value: boolean) => void;
}

function SendRecieveButton({ isSend, setIsSend }: SendReceiveButtonProps) {
  return (
    <div>
      <div className="flex items-center gap-6 border-b border-line">
        <button
          onClick={() => setIsSend(true)}
          className={`cursor-pointer border-b-2 py-3 text-sm font-medium transition-colors duration-150 ${
            isSend
              ? "border-mint text-chalk"
              : "border-transparent text-mist hover:text-chalk"
          }`}
        >
          Send
        </button>
        <button
          onClick={() => setIsSend(false)}
          className={`cursor-pointer border-b-2 py-3 text-sm font-medium transition-colors duration-150 ${
            !isSend
              ? "border-mint text-chalk"
              : "border-transparent text-mist hover:text-chalk"
          }`}
        >
          Recieve
        </button>
      </div>
      <p className="mt-3 text-sm text-mist">
        {isSend
          ? "Upload and share a temporary code."
          : "Enter a code to retrieve shared files."}
      </p>
    </div>
  );
}

export default SendRecieveButton;
