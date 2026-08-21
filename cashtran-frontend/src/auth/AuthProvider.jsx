import React, { createContext, useState, useEffect } from "react";
import { authLogin, authRegister } from "../api/authApi";

export const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem("cashtran_user");

    return raw ? JSON.parse(raw) : null;
  });

  useEffect(() => {
    if (user) {
      localStorage.setItem("cashtran_user", JSON.stringify(user));
    } else {
      localStorage.removeItem("cashtran_user");
      localStorage.removeItem("cashtran_token");
    }
  }, [user]);

  async function login(username, password) {
    const resp = await authLogin(username, password);

    const token = resp.data.token;

    localStorage.setItem("cashtran_token", token);

    setUser(resp.data.user);

    return resp;
  }

  // UPDATED: Added email parameter
  async function register(username, password, email) {
    return authRegister(username, password, email);
  }
  function updateUser(updatedUser) {
    setUser(updatedUser);
  }

  function logout() {
    setUser(null);

    localStorage.removeItem("cashtran_token");
  }

  return (
    <AuthContext.Provider
      value={{
        user,
        login,
        register,
        logout,
        updateUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}
