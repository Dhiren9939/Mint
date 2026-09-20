import { useState } from "react";
import SendReceiveButton from "./SendRecieveButton";
import RecieveCard from "./RecieveCard";
import SendCard from "./SendCard";

function Home() {
    const [isSend, setIsSend] = useState(true);

    return (
        <section className="animate-fade-up">
            <div className="flex flex-col gap-5">
                <h1 className="text-lg font-medium leading-snug text-mist">
                    Send files and text with temporary retrieval keys
                </h1>

                <div className="flex flex-col gap-6">
                    <SendReceiveButton isSend={isSend} setIsSend={setIsSend} />
                    <div>{isSend ? <SendCard /> : <RecieveCard />}</div>
                </div>
            </div>
        </section>
    );
}

export default Home;
