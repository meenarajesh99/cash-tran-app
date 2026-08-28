import api from "./axiosClient";
import axiosClient from "./axiosClient";

export const authLogin = (username, password) =>
  api.post("/api/auth/login", { username, password });

export const authMfaLogin = (mfaToken, code) =>
  api.post("/api/auth/mfa/login", {
    mfaToken,
    code,
  });

export const verifyMfaLogin = async (mfaToken, code) => {
  return axiosClient.post("/api/auth/mfa/login", {
    mfaToken,
    code,
  });
};

export const authRegister = (username, password, email) =>
  api.post("/api/auth/register", { username, password, email });

export const getBalance = () => api.get("/api/balance"); // if you followed backend change, this will be /api/balance; if you want /api/accounts/balance change accordingly

export const getUsers = () => api.get("/api/users");

export const getTransfers = () => api.get("/api/transfers");

export const getTransfer = (id) => api.get(`/api/transfers/${id}`);

export const sendTransfer = (userId, amount) =>
  api.post("/api/transfers/send", { userId, amount });

export const createMoneyRequest = (userId, amount) =>
  api.post("/api/requests", {
    userId,
    amount,
  });

export const getPendingReceivedTransfers = () =>
  api.get("/api/transfers/pending/received");

export const getPendingSentTransfers = () =>
  api.get("/api/transfers/pending/sent");

export const approveTransfer = (transferId) => {
  return api.put(`/api/transfers/${transferId}/approve`);
};

export const rejectTransfer = (transferId) => {
  return api.put(`/api/transfers/${transferId}/reject`);
};

export const updateEmail = async (email) => {
  const response = await axiosClient.put("/api/account/email", {
    email,
  });
  return response.data;
};

export const forgotPassword = async (email) => {
  return axiosClient.post("/api/auth/forgot-password", {
    email,
  });
};

export const resetPassword = async (token, password) => {
  return axiosClient.post("/api/auth/reset-password", {
    token,
    password,
  });
};
export const downloadTransactionHistory = async () => {
  return api.get("/api/transfers/statement", {
    responseType: "blob",
  });
};
export default api;
