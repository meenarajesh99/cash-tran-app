import { useState } from "react";
import { useNavigate } from "react-router-dom";

import axiosClient from "../api/axiosClient";

export default function MfaLogin() {
  const [code, setCode] = useState("");

  const navigate = useNavigate();

  const verifyMfa = async () => {
    const username = sessionStorage.getItem("mfa_username");

    const response = await axiosClient.post("/api/auth/mfa/login", {
      username,
      code,
    });

    localStorage.setItem("cashtran_token", response.data.token);

    localStorage.setItem("cashtran_user", JSON.stringify(response.data.user));

    sessionStorage.removeItem("mfa_username");

    navigate("/dashboard");
  };

  return (
    <div>
      <h2>Two-Factor Authentication</h2>

      <p>Enter the code from Google Authenticator.</p>

      <input
        value={code}
        onChange={(e) => setCode(e.target.value)}
        placeholder="123456"
      />

      <button onClick={verifyMfa}>Verify</button>
    </div>
  );
}
