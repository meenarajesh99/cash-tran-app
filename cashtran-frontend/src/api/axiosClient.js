// cashtran-frontend/src/api/axiosClient.js
import axios from "axios";

const baseURL = import.meta.env.VITE_API_URL || "http://localhost:8080";

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

export default api;
