import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { forgotPassword } from "../api/authApi";
import {
  Alert,
  Box,
  Button,
  Paper,
  TextField,
  Typography,
} from "@mui/material";

export default function ForgotPassword() {
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState("");
  const [error, setError] = useState("");

  async function handleSubmit(event) {
    event.preventDefault();

    setSuccess("");
    setError("");

    if (!email.trim()) {
      setError("Email is required.");
      return;
    }

    setLoading(true);

    try {
      await forgotPassword(email.trim());

      setSuccess(
        "If an account exists with this email, password reset instructions will be sent.",
      );

      setEmail("");
    } catch (err) {
      console.error("Forgot password error:", err);

      setError(
        err?.response?.data?.message ||
          err?.response?.data ||
          "Unable to process password reset request.",
      );
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
        <Typography
          variant="h4"
          fontWeight="bold"
          textAlign="center"
          gutterBottom
        >
          Forgot Password
        </Typography>

        <Typography color="text.secondary" textAlign="center" sx={{ mb: 3 }}>
          Enter your email address to reset your password.
        </Typography>

        {success && (
          <Alert severity="success" sx={{ mb: 2 }}>
            {success}
          </Alert>
        )}

        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        <Box component="form" onSubmit={handleSubmit}>
          <TextField
            fullWidth
            label="Email"
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            required
            margin="normal"
          />

          <Button
            type="submit"
            fullWidth
            variant="contained"
            disabled={loading}
            sx={{
              mt: 3,
              py: 1.5,
              borderRadius: 3,
            }}
          >
            {loading ? "Sending..." : "Reset Password"}
          </Button>

          <Button
            type="button"
            fullWidth
            variant="outlined"
            onClick={() => navigate("/login")}
            sx={{
              mt: 2,
              py: 1.5,
              borderRadius: 3,
            }}
          >
            Back to Login
          </Button>
        </Box>
      </Paper>
    </Box>
  );
}
