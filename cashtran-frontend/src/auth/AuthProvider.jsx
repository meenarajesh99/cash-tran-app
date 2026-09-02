/* eslint-disable react-refresh/only-export-components */
import { createContext, useState, useEffect } from "react";
import {
  authLogin,
  authMfaLogin,
  authRegister,
  verifyMfaSetup,
} from "../api/authApi";

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

    /*
     * MFA is required.
     *
     * Do NOT store the MFA token as the normal
     * cashtran_token.
     */
    if (resp.data.mfaRequired) {
      return {
        ...resp,
        mfaRequired: true,
        mfaToken: resp.data.mfaToken,
      };
    }

    /*
     * User does not have MFA enabled.
     * This is the normal login flow.
     */
    const token = resp.data.token;

    localStorage.setItem("cashtran_token", token);

    setUser(resp.data.user);

    return {
      ...resp,
      mfaRequired: false,
    };
  }

  async function verifyMfa(mfaToken, code) {
    const resp = await authMfaLogin(mfaToken, code);

    /*
     * This is the REAL JWT.
     */
    const token = resp.data.token;

    localStorage.setItem("cashtran_token", token);

    setUser(resp.data.user);

    return resp;
  }
  async function completeMfaSetup(enrollmentToken, code) {
    return verifyMfaSetup(enrollmentToken, code);
  }

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
        verifyMfa,
        register,
        logout,
        updateUser,
        completeMfaSetup,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}
