// cashtran-frontend/src/api/axiosClient.js
import axios from "axios";

const baseURL = import.meta.env.VITE_API_URL || "";

const api = axios.create({
  baseURL,
  headers: {
    "Content-Type": "application/json",
  },
  // optional timeout
  timeout: 10000,
});

// Attach token automatically
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("cashtran_token");
    if (token) {
      config.headers["Authorization"] = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

// Global response handler: on 401/403 clear token and redirect to login
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error && error.response) {
      const status = error.response.status;
      if (status === 401 || status === 403) {
        if (typeof localStorage !== "undefined") {
          localStorage.removeItem("cashtran_token");
        }

        // Only attempt full navigation in browser environments
        if (typeof window !== "undefined" && window.location) {
          window.location.href = "/login";
        }
      }
    }

    return Promise.reject(error);
  },
);

export default api;
