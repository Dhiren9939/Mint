import { Toaster } from "react-hot-toast";

interface ToastProps {
  isMobile: boolean;
}

function Toast({ isMobile }: ToastProps) {
  return (
    <Toaster
      position={isMobile ? "bottom-center" : "top-right"}
      toastOptions={{
        style: {
          borderRadius: "4px",
          background: "#111a17",
          color: "#e8f0ec",
          border: "1px solid #223029",
        },
        success: {
          style: {
            background: "#111a17",
            border: "1px solid #00bc7d",
            color: "#e8f0ec",
          },
          iconTheme: {
            primary: "#00bc7d",
            secondary: "#111a17",
          },
        },
        error: {
          style: {
            background: "#111a17",
            border: "1px solid #e2564b",
            color: "#e8f0ec",
          },
          iconTheme: {
            primary: "#e2564b",
            secondary: "#111a17",
          },
        },
        loading: {
          style: {
            background: "#111a17",
            border: "1px solid #223029",
            color: "#e8f0ec",
          },
          iconTheme: {
            primary: "#00bc7d",
            secondary: "#111a17",
          },
        },
      }}
    />
  );
}

export default Toast;
