import React from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";

import Login from "./pages/Login";
import Register from "./pages/Register";
import Dashboard from "./pages/Dashboard";

import Users from "./pages/Users";
import Transfers from "./pages/Transfers";
import SendTransfer from "./pages/SendTransfer";

import { AuthContext } from "./auth/AuthProvider";

import "./App.css";

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
              <LoginPage />
            </PublicRoute>
          }
        />

        <Route
          path="/register"

          element={
            <PublicRoute>
              <RegisterPage />
            </PublicRoute>
          }
        />

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
          path="/users"

          element={
            <ProtectedRoute>
              <UsersPage />
            </ProtectedRoute>
          }
        />

        <Route
          path="/transfers"

          element={
            <ProtectedRoute>
              <TransfersPage />
            </ProtectedRoute>
          }
        />

        <Route
          path="/transfer/send"

          element={
            <ProtectedRoute>
              <SendMoneyPage />
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
