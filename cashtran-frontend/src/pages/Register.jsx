import React, { useState } from "react";
import { useNavigate } from "react-router-dom";

import {
  Box,
  Paper,
  Typography,
  TextField,
  Button,
  Alert,
  InputAdornment,
  IconButton,
  LinearProgress,
  Link,
  Stack,
} from "@mui/material";

import {
  Person,
  Email,
  Lock,
  Visibility,
  VisibilityOff,
  AccountBalanceWallet,
} from "@mui/icons-material";

import { AuthContext } from "../auth/AuthProvider";

export default function RegisterPage() {
  /*Get the register function from the application's authentication context.*/
  const { register } = React.useContext(AuthContext);

  const navigate = useNavigate();

  const [username, setUsername] = useState("");

  const [email, setEmail] = useState("");

  const [password, setPassword] = useState("");

  const [confirmPassword, setConfirmPassword] = useState("");

  const [showPassword, setShowPassword] = useState(false);

  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const [loading, setLoading] = useState(false);

  const [error, setError] = useState(null);

  const [success, setSuccess] = useState(null);

  function passwordStrength(value) {
    let strength = 0;

    if (value.length >= 6) strength += 25;

    if (/[A-Z]/.test(value)) strength += 25;

    if (/[0-9]/.test(value)) strength += 25;

    if (/[^A-Za-z0-9]/.test(value)) strength += 25;

    return strength;
  }

  async function onSubmit(e) {
    /*Normally, submitting an HTML form causes the browser to reload/navigate. React doesn't want that and says Stop the browser's normal form submission.
       I'll handle it using JavaScript. */
    e.preventDefault();

    setError(null);

    setSuccess(null);

    if (password !== confirmPassword) {
      setError("Passwords do not match");

      return;
    }

    if (password.length < 6) {
      setError("Password must be at least 6 characters");

      return;
    }

    try {
      setLoading(true);

      await register(username, password, email);

      setSuccess("Registration successful. Redirecting to login...");

      setTimeout(() => {
        navigate("/login");
      }, 1500);
    } catch (err) {
      setError(err?.response?.data || err.message || "Registration failed");
    } finally {
      setLoading(false);
    }
  }
  /* Everything inside this describes what React should display - the visual part */
  return (
    <Box
      sx={{
        minHeight: "100vh",
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
        p: 2,
        background: "linear-gradient(135deg,#1565c0,#42a5f5)",
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
        <Box textAlign="center" mb={4}>
          <AccountBalanceWallet
            sx={{
              fontSize: 70,
              color: "primary.main",
            }}
          />

          <Typography variant="h4" fontWeight="bold">
            Create Account
          </Typography>

          <Typography color="text.secondary">
            Join CashTran secure payment network
          </Typography>
        </Box>

        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {String(error)}
          </Alert>
        )}

        {success && (
          <Alert severity="success" sx={{ mb: 2 }}>
            {success}
          </Alert>
        )}

        <Box component="form" onSubmit={onSubmit}>
          {/* Username */}

          <TextField
            fullWidth
            label="Username"
            margin="normal"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <Person />
                </InputAdornment>
              ),
            }}
          />

          {/* Email */}

          <TextField
            fullWidth
            label="Email"
            margin="normal"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <Email />
                </InputAdornment>
              ),
            }}
          />

          {/* Password */}

          <TextField
            fullWidth
            label="Password"
            margin="normal"
            type={showPassword ? "text" : "password"}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <Lock />
                </InputAdornment>
              ),

              endAdornment: (
                <InputAdornment position="end">
                  <IconButton onClick={() => setShowPassword(!showPassword)}>
                    {showPassword ? <VisibilityOff /> : <Visibility />}
                  </IconButton>
                </InputAdornment>
              ),
            }}
          />

          {password && (
            <Box mt={1}>
              <Typography variant="caption">Password strength</Typography>

              <LinearProgress
                variant="determinate"
                value={passwordStrength(password)}
              />
            </Box>
          )}

          {/* Confirm Password */}

          <TextField
            fullWidth
            label="Confirm Password"
            margin="normal"
            type={showConfirmPassword ? "text" : "password"}
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            required
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <Lock />
                </InputAdornment>
              ),

              endAdornment: (
                <InputAdornment position="end">
                  <IconButton
                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  >
                    {showConfirmPassword ? <VisibilityOff /> : <Visibility />}
                  </IconButton>
                </InputAdornment>
              ),
            }}
          />

          <Button
            fullWidth
            type="submit"
            variant="contained"
            size="large"
            disabled={loading}
            sx={{
              mt: 4,
              py: 1.5,
              borderRadius: 3,
              fontWeight: "bold",
            }}
          >
            {loading ? "Creating Account..." : "Register"}
          </Button>
        </Box>

        <Stack direction="row" justifyContent="center" mt={4} spacing={1}>
          <Typography color="text.secondary">
            Already have an account?
          </Typography>

          <Link
            component="button"
            underline="hover"
            onClick={() => navigate("/login")}
          >
            Login
          </Link>
        </Stack>
      </Paper>
    </Box>
  );
}
