import { useState } from "react";
import SendReceiveButton from "./SendRecieveButton";
import RecieveCard from "./RecieveCard";
import SendCard from "./SendCard";

function Home() {
  const [isSend, setIsSend] = useState(true);

  return (
    <div className="animate-fade-in-up">
      <SendReceiveButton isSend={isSend} setIsSend={setIsSend} />
      {isSend ? <SendCard /> : <RecieveCard />}
    </div>
  );
}

export default Home;
