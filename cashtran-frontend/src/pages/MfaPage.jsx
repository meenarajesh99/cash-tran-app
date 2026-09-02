import { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";

import {
  Box,
  Paper,
  Typography,
  TextField,
  Button,
  Alert,
} from "@mui/material";

import AccountBalanceWalletIcon from "@mui/icons-material/AccountBalanceWallet";

import CircularProgress from "@mui/material/CircularProgress";

import { verifyMfaLogin } from "../api/authApi";

export default function MfaPage() {
  const location = useLocation();
  const navigate = useNavigate();

  const { username, mfaToken } = location.state || {};

  const [code, setCode] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function handleSubmit(e) {
    e.preventDefault();

    setError("");

    if (!mfaToken) {
      setError("MFA session is missing. Please log in again.");
      return;
    }

    if (!/^\d{6}$/.test(code)) {
      setError("Enter the 6-digit code from Google Authenticator.");
      return;
    }

    setLoading(true);

    try {
      const response = await verifyMfaLogin(mfaToken, code);

      /*
       * MFA succeeded.
       *
       * This is now the REAL JWT.
       */
      localStorage.setItem("cashtran_token", response.data.token);

      localStorage.setItem("cashtran_user", JSON.stringify(response.data.user));

      navigate("/dashboard");
    } catch (err) {
      setError(err?.response?.data || "Invalid or expired MFA code.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <Box
      sx={{
        minHeight: "100vh",
        background: "linear-gradient(135deg,#1565c0,#42a5f5)",
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
        p: 2,
      }}
    >
      <Paper
        elevation={10}
        sx={{
          width: 430,
          p: 5,
          borderRadius: 5,
        }}
      >
        <Box sx={{ textAlign: "center", mb: 4 }}>
          <AccountBalanceWalletIcon
            sx={{
              fontSize: 70,
              color: "#1976d2",
            }}
          />

          <Typography variant="h4" fontWeight="bold" sx={{ mt: 1 }}>
            CashTran
          </Typography>

          <Typography color="text.secondary">
            Two-Factor Authentication
          </Typography>
        </Box>

        {error && (
          <Alert severity="error" sx={{ mb: 3 }}>
            {String(error)}
          </Alert>
        )}

        <Typography align="center" sx={{ mb: 3 }}>
          Enter the 6-digit verification code from Google Authenticator.
        </Typography>

        <Typography align="center" fontWeight="bold" sx={{ mb: 3 }}>
          {username}
        </Typography>

        <Box component="form" onSubmit={handleSubmit}>
          <TextField
            fullWidth
            label="Authenticator Code"
            value={code}
            onChange={(e) =>
              setCode(e.target.value.replace(/\D/g, "").slice(0, 6))
            }
            inputProps={{
              maxLength: 6,
              inputMode: "numeric",
            }}
            autoFocus
          />

          <Button
            fullWidth
            variant="contained"
            size="large"
            type="submit"
            disabled={loading || code.length !== 6}
            sx={{
              mt: 3,
              py: 1.5,
              borderRadius: 3,
              fontWeight: "bold",
            }}
          >
            {loading ? (
              <CircularProgress color="inherit" size={28} />
            ) : (
              "Verify Code"
            )}
          </Button>

          <Button
            fullWidth
            variant="text"
            sx={{ mt: 2 }}
            onClick={() => navigate("/login")}
          >
            Back to Login
          </Button>
        </Box>
      </Paper>
    </Box>
  );
}
