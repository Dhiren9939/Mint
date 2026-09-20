import { useState } from "react";
import SendReceiveButton from "./SendRecieveButton";
import RecieveCard from "./RecieveCard";
import SendCard from "./SendCard";

function Home() {
    const [isSend, setIsSend] = useState(true);

    return (
        <section className="animate-fade-up">
            <div className="flex flex-col gap-8">
                <div className="space-y-2">
                    <h1 className="text-[clamp(1.75rem,4vw,2.25rem)] font-semibold leading-tight tracking-tight text-chalk">
                        Send files and text with temporary retrieval keys
                    </h1>
                    <p className="max-w-[62ch] text-sm leading-relaxed text-mist sm:text-base">
                        No account, no trace. Upload once, share a six-character
                        code, and let the link expire on its own.
                    </p>
                </div>

                <div className="flex flex-col gap-6">
                    <SendReceiveButton isSend={isSend} setIsSend={setIsSend} />
                    <div>{isSend ? <SendCard /> : <RecieveCard />}</div>
                </div>
            </div>
        </section>
    );
}

export default Home;
