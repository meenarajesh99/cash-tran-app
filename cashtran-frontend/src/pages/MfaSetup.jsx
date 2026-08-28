import { useState } from "react";
import { QRCodeSVG } from "qrcode.react";
import axiosClient from "../api/axiosClient";

export default function MfaSetup() {
  const [otpAuthUrl, setOtpAuthUrl] = useState(null);

  const [code, setCode] = useState("");

  const setupMfa = async () => {
    const response = await axiosClient.post("/api/mfa/setup");

    setOtpAuthUrl(response.data.otpAuthUrl);
  };

  const verifyMfa = async () => {
    await axiosClient.post("/api/mfa/verify-setup", {
      code,
    });

    alert("MFA enabled!");
  };

  return (
    <div>
      <h2>Enable Two-Factor Authentication</h2>

      {!otpAuthUrl && <button onClick={setupMfa}>Enable MFA</button>}

      {otpAuthUrl && (
        <>
          <p>Scan this QR code using Google Authenticator</p>

          <QRCodeSVG value={otpAuthUrl} size={250} />

          <input
            value={code}
            onChange={(e) => setCode(e.target.value)}
            placeholder="Enter 6-digit code"
          />

          <button onClick={verifyMfa}>Verify and Enable</button>
        </>
      )}
    </div>
  );
}
