import React, { useContext, useState } from "react";
import { useNavigate } from "react-router-dom";

import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Paper,
  TextField,
  Typography,
} from "@mui/material";

import { AuthContext } from "../auth/AuthProvider";
import { updateEmail } from "../api/authApi";

export default function MyAccount() {
  const { user, updateUser } = useContext(AuthContext);

  const [email, setEmail] = useState(user?.email || "");
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState("");
  const [error, setError] = useState("");
  const navigate = useNavigate();

  async function handleSubmit(event) {
    event.preventDefault();
    console.log("A. handleSubmit called");
    console.log("B. email:", email);

    setSuccess("");
    setError("");

    if (!email.trim()) {
      setError("Email is required.");
      return;
    }

    setLoading(true);

    try {
      console.log("C. before updateEmail");
      const response = await updateEmail(email.trim());
      console.log("D. updateEmail returned:", response);
      updateUser(response.data);

      setSuccess("Email updated successfully.");
    } catch (err) {
      console.error("E. UPDATE EMAIL ERROR:", err);

      setError(
        err?.response?.data?.message ||
          err?.response?.data?.error ||
          "Unable to update email.",
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <Box
      sx={{
        minHeight: "100vh",
        background: "linear-gradient(135deg,#e3f2fd,#ffffff)",
        py: 5,
      }}
    >
      <Paper
        sx={{
          maxWidth: 600,
          mx: "auto",
          p: 4,
          borderRadius: 4,
        }}
      >
        <Typography variant="h4" fontWeight="bold" gutterBottom>
          My Account
        </Typography>

        <Typography color="text.secondary" sx={{ mb: 3 }}>
          Manage your CashTran account information.
        </Typography>

        <TextField
          fullWidth
          label="Username"
          value={user?.username || ""}
          disabled
          margin="normal"
        />

        <Box component="form" onSubmit={handleSubmit}>
          <TextField
            fullWidth
            label="Email"
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            margin="normal"
          />

          {success && (
            <Alert severity="success" sx={{ mt: 2 }}>
              {success}
            </Alert>
          )}

          {error && (
            <Alert severity="error" sx={{ mt: 2 }}>
              {String(error)}
            </Alert>
          )}

          <Button
            type="submit"
            variant="contained"
            fullWidth
            disabled={loading}
            aria-label="Update Email"
            sx={{
              mt: 3,
              py: 1.5,
              borderRadius: 3,
            }}
          >
            {loading ? (
              <CircularProgress size={24} aria-label="Updating email" />
            ) : (
              "Update Email"
            )}
          </Button>
          <Button
            type="button"
            variant="outlined"
            fullWidth
            onClick={() => navigate("/dashboard")}
            sx={{
              mt: 2,
              py: 1.5,
              borderRadius: 3,
            }}
          >
            Back to Dashboard
          </Button>
        </Box>
      </Paper>
    </Box>
  );
}
