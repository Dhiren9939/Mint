import React, { useState } from "react";
import Navbar from "./components/Navbar";
import SendReceiveButton from "./components/SendRecieveButton";
import RecieveCard from "./components/RecieveCard";
import SendCard from "./components/SendContent";

function App() {
  const [isSend, setIsSend] = useState(true);

  return (
    <div>
      <Navbar />
      <div className="px-6 font-['Inter']">
        <SendReceiveButton isSend={isSend} setIsSend={setIsSend} />
        {isSend ? <SendCard /> : <RecieveCard />}
      </div>
    </div>
  );
}

export default App;
