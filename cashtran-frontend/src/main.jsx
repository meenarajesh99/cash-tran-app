import React from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import App from "./App";
import { AuthProvider, AuthContext } from "./auth/AuthProvider";
import LoginPage from "./pages/Login";
import RegisterPage from "./pages/Register";
import Dashboard from "./pages/Dashboard";
import UsersPage from "./pages/Users";
import SendTransfer from "./pages/SendTransfer";
import TransfersPage from "./pages/Transfers";

createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route
            path="/dashboard"
            element={
              <Protected>
                <Dashboard />
              </Protected>
            }
          />
          <Route
            path="/users"
            element={
              <Protected>
                <UsersPage />
              </Protected>
            }
          />
          <Route
            path="/transfer/send"
            element={
              <Protected>
                <SendTransfer />
              </Protected>
            }
          />
          <Route
            path="/transfers"
            element={
              <Protected>
                <TransfersPage />
              </Protected>
            }
          />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  </React.StrictMode>,
);

// Protected wrapper
function Protected({ children }) {
  const { user } = React.useContext(AuthContext);
  if (!user) return <Navigate to="/login" replace />;
  return children;
}
