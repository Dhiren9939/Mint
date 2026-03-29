interface SendReceiveButtonProps {
  isSend: boolean;
  setIsSend: (value: boolean) => void;
}

interface ButtonProps {
  handleClick: () => void;
  isSend: boolean;
  currState: boolean;
}

function Button({ handleClick, isSend, currState }: ButtonProps) {
  const active = isSend === currState;

  return (
    <button
      onClick={handleClick}
      className={`${active ? "bg-emerald-500 text-slate-800 shadow-2xl" : "text-slate-400 py-2"} font-medium text-center w-full rounded-2xl transition-colors duration-300 ease-in-out`}
    >
      {isSend ? "Send" : "Recieve"}
    </button>
  );
}

function SendRecieveButton({ isSend, setIsSend }: SendReceiveButtonProps) {
  return (
    <div className="text-xl py-6 flex flex-col items-center">
      <div className="w-full min-w-72 sm:w-fit flex justify-center gap-2 bg-slate-800 p-2 rounded-2xl border border-slate-700 shadow-2xs">
        <Button
          isSend={true}
          currState={isSend}
          handleClick={() => {
            setIsSend(true);
          }}
        />
        <Button
          isSend={false}
          currState={isSend}
          handleClick={() => {
            setIsSend(false);
          }}
        />
      </div>
    </div>
  );
}

export default SendRecieveButton;
