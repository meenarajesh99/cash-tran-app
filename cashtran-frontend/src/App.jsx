import React from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";

import Login from "./pages/Login";
import Register from "./pages/Register";
import Dashboard from "./pages/Dashboard";
import ForgotPassword from "./pages/ForgotPassword";
import ResetPassword from "./pages/ResetPassword";

import Users from "./pages/Users";
import Transfers from "./pages/Transfers";
import SendTransfer from "./pages/SendTransfer";
import MyAccount from "./pages/MyAccount";

import { AuthContext } from "./auth/AuthProvider";

import "./App.css";
import RequestMoney from "./pages/RequestMoney.jsx";
import MfaLogin from "./pages/MfaLogin.jsx";
import MfaSetup from "./pages/MfaSetup.jsx";
import MfaPage from "./pages/MfaPage";

function ProtectedRoute({ children }) {
  const { user } = React.useContext(AuthContext);

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  return children;
}

function PublicRoute({ children }) {
  const { user } = React.useContext(AuthContext);

  if (user) {
    return <Navigate to="/dashboard" replace />;
  }

  return children;
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public Routes */}

        <Route
          path="/login"

          element={
            <PublicRoute>
              <Login />
            </PublicRoute>
          }
        />

        <Route
          path="/register"

          element={
            <PublicRoute>
              <Register />
            </PublicRoute>
          }
        />
        <Route
          path="/forgot-password"
          element={
            <PublicRoute>
              <ForgotPassword />
            </PublicRoute>
          }
        />
        <Route
          path="/reset-password"
          element={
            <PublicRoute>
              <ResetPassword />
            </PublicRoute>
          }
        />
        <Route path="/mfa" element={<MfaLogin />} />

        <Route path="/mfa/setup" element={<MfaSetup />} />
        <Route path="/mfa" element={<MfaPage />} />

        {/* Protected Routes */}

        <Route
          path="/dashboard"

          element={
            <ProtectedRoute>
              <Dashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="/account"
          element={
            <ProtectedRoute>
              <MyAccount />
            </ProtectedRoute>
          }
        />

        <Route
          path="/users"

          element={
            <ProtectedRoute>
              <Users />
            </ProtectedRoute>
          }
        />

        <Route
          path="/transfers"

          element={
            <ProtectedRoute>
              <Transfers />
            </ProtectedRoute>
          }
        />

        <Route
          path="/transfer/send"

          element={
            <ProtectedRoute>
              <SendTransfer />
            </ProtectedRoute>
          }
        />

        <Route
          path="/request-money"

          element={
            <ProtectedRoute>
              <RequestMoney />
            </ProtectedRoute>
          }
        />

        {/* Default Route */}

        <Route
          path="/"

          element={
            <Navigate
              to="/dashboard"

              replace
            />
          }
        />

        {/* Catch Unknown URLs */}

        <Route
          path="*"

          element={
            <Navigate
              to="/dashboard"

              replace
            />
          }
        />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
