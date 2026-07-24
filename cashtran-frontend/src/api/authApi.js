import api from "./axiosClient";

export const authLogin = (username, password) =>
  api.post("/api/auth/login", { username, password });

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
