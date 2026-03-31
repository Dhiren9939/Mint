import axios from "axios";

export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data?: T;
  error?: ApiError;
}

export interface ApiError {
  status: number;
  code: string;
  message: string;
  fields?: Record<string, unknown>;
}

export const ExpiryDuration = {
  MINUTES15: "MINUTES15",
  MINUTES30: "MINUTES30",
  MINUTES60: "MINUTES60",
  HOURS24: "HOURS24",
} as const;

export type ExpiryDuration =
  (typeof ExpiryDuration)[keyof typeof ExpiryDuration];

export type FileState = "PENDING" | "READY" | "DELETED";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  withCredentials: true,
});

export default api;
