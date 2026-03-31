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
          borderRadius: "12px",
          background: "#1e293b",
          color: "#e2e8f0",
          border: "1px solid #334155",
          boxShadow: "0 4px 6px -1px rgba(0, 0, 0, 0.3)",
        },
        success: {
          style: {
            background: "#1e293b",
            border: "1px solid #10b981",
            color: "#34d399",
          },
          iconTheme: {
            primary: "#10b981",
            secondary: "#1e293b",
          },
        },
        error: {
          style: {
            background: "#1e293b",
            border: "1px solid #f43f5e",
            color: "#fb7185",
          },
          iconTheme: {
            primary: "#f43f5e",
            secondary: "#1e293b",
          },
        },
        loading: {
          style: {
            background: "#1e293b",
            border: "1px solid #334155",
            color: "#e2e8f0",
          },
          iconTheme: {
            primary: "#10b981",
            secondary: "#1e293b",
          },
        },
      }}
    />
  );
}

export default Toast;
