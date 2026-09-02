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
  Divider,
} from "@mui/material";

import {
  Person,
  Email,
  Lock,
  Visibility,
  VisibilityOff,
  AccountBalanceWallet,
  Security,
} from "@mui/icons-material";

import { QRCodeSVG } from "qrcode.react";

import { AuthContext } from "../auth/AuthProvider";

export default function Register() {
  const { register, completeMfaSetup } = React.useContext(AuthContext);

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

  /*
   * MFA enrollment state
   */
  const [mfaSetupUrl, setMfaSetupUrl] = useState(null);
  const [enrollmentToken, setEnrollmentToken] = useState(null);
  const [mfaCode, setMfaCode] = useState("");
  const [mfaSetupError, setMfaSetupError] = useState(null);

  function passwordStrength(value) {
    let strength = 0;

    if (value.length >= 6) strength += 25;
    if (/[A-Z]/.test(value)) strength += 25;
    if (/[0-9]/.test(value)) strength += 25;
    if (/[^A-Za-z0-9]/.test(value)) strength += 25;

    return strength;
  }

  /*
   * ============================================================
   * REGISTRATION
   * ============================================================
   */
  async function onSubmit(e) {
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

      /*
       * Register the user.
       *
       * The backend should return:
       *
       * {
       *   ...
       *   mfaRequired: true,
       *   mfaSetupUrl: "otpauth://...",
       *   enrollmentToken: "..."
       * }
       */
      const response = await register(username, password, email);

      // Support multiple register response shapes used across tests and
      // environments: some register implementations return { data: { ... } }
      // while tests may mock a plain object. Normalize to `data`.
      const data = response?.data ?? response ?? {};

      // If the backend indicates MFA enrollment is required (or provides an
      // mfaSetupUrl), switch to the MFA flow. Otherwise treat registration
      // as complete and redirect the user to login.
      if (data.mfaSetupUrl || data.mfaRequired) {
        setMfaSetupUrl(data.mfaSetupUrl);
        setEnrollmentToken(data.enrollmentToken);

        setSuccess(
          "Account created successfully. Please set up two-factor authentication.",
        );
      } else {
        setSuccess("Registration successful. Redirecting to login...");

        // Give the user a short moment to read the success message, then
        // redirect to the login page.
        setTimeout(() => navigate("/login"), 1500);
      }
    } catch (err) {
      setError(err?.response?.data || err.message || "Registration failed");
    } finally {
      setLoading(false);
    }
  }

  /*
   * ============================================================
   * MFA ENROLLMENT
   * ============================================================
   */
  async function handleMfaSetup(e) {
    e.preventDefault();

    setMfaSetupError(null);
    setSuccess(null);

    if (!mfaCode || mfaCode.length !== 6) {
      setMfaSetupError("Please enter the 6-digit authentication code.");
      return;
    }

    if (!enrollmentToken) {
      setMfaSetupError(
        "MFA enrollment session is missing. Please register again.",
      );
      return;
    }

    try {
      setLoading(true);

      await completeMfaSetup(enrollmentToken, mfaCode);

      setSuccess("MFA setup completed successfully. Redirecting to login...");

      /*
       * MFA is now enabled in the database.
       *
       * The user can log in normally, and the login
       * process will automatically require their
       * Google Authenticator code.
       */
      setTimeout(() => {
        navigate("/login");
      }, 1500);
    } catch (err) {
      setMfaSetupError(
        err?.response?.data ||
          err.message ||
          "Unable to verify authentication code.",
      );
    } finally {
      setLoading(false);
    }
  }

  /*
   * ============================================================
   * MFA SETUP SCREEN
   * ============================================================
   */
  if (mfaSetupUrl) {
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
          {/* Header */}
          <Box sx={{ textAlign: "center", mb: 3 }}>
            <Security
              sx={{
                fontSize: 65,
                color: "primary.main",
              }}
            />

            <Typography variant="h4" fontWeight="bold" sx={{ mt: 1 }}>
              Secure Your Account
            </Typography>

            <Typography color="text.secondary" sx={{ mt: 1 }}>
              Set up Google Authenticator to protect your CashTran account.
            </Typography>
          </Box>

          {success && (
            <Alert severity="success" sx={{ mb: 3 }}>
              {String(success)}
            </Alert>
          )}

          {mfaSetupError && (
            <Alert severity="error" sx={{ mb: 3 }}>
              {String(mfaSetupError)}
            </Alert>
          )}

          {/* Step 1 */}
           <Typography variant="subtitle1" fontWeight="bold" align="center" sx={{ mb: 1 }}>
            Step 1: Scan the QR Code
          </Typography>

           <Typography variant="body2" color="text.secondary" align="center" sx={{ mb: 2 }}>
            Open Google Authenticator on your phone and scan this QR code.
          </Typography>

          {/* QR Code */}
          <Box
            sx={{
              display: "flex",
              justifyContent: "center",
              alignItems: "center",
              p: 2,
              mb: 3,
            }}
          >
            <QRCodeSVG value={mfaSetupUrl} size={220} level="M" />
          </Box>

          <Divider sx={{ mb: 3 }} />

          {/* Step 2 */}
           <Typography variant="subtitle1" fontWeight="bold" align="center" sx={{ mb: 1 }}>
            Step 2: Enter Your Code
          </Typography>

           <Typography variant="body2" color="text.secondary" align="center" sx={{ mb: 2 }}>
            Enter the 6-digit code displayed in Google Authenticator.
          </Typography>

          <Box component="form" onSubmit={handleMfaSetup}>
            <TextField
              fullWidth
              label="Authentication Code"
              value={mfaCode}
              onChange={(e) => {
                const value = e.target.value.replace(/\D/g, "").slice(0, 6);

                setMfaCode(value);
              }}
              inputProps={{
                maxLength: 6,
                inputMode: "numeric",
                autoComplete: "one-time-code",
              }}
              placeholder="123456"
              autoFocus
              sx={{
                mb: 3,
                "& input": {
                  textAlign: "center",
                  letterSpacing: "0.5rem",
                  fontSize: "1.5rem",
                },
              }}
            />

            <Button
              fullWidth
              variant="contained"
              size="large"
              type="submit"
              disabled={loading || mfaCode.length !== 6}
              sx={{
                py: 1.5,
                borderRadius: 3,
                fontWeight: "bold",
              }}
            >
              {loading ? "Verifying..." : "Verify & Complete Registration"}
            </Button>

            <Button
              fullWidth
              variant="text"
              onClick={() => navigate("/login")}
              disabled={loading}
              sx={{
                mt: 2,
                borderRadius: 3,
              }}
            >
              ← Back to Login
            </Button>
          </Box>

          <Typography mt={4} align="center" color="text.secondary" fontSize={13}>
            Fast • Secure • Trusted Payments
          </Typography>

          <Typography align="center" color="text.secondary" fontSize={12}>
            © {new Date().getFullYear()} CashTran. All Rights Reserved.
          </Typography>
        </Paper>
      </Box>
    );
  }

  /*
   * ============================================================
   * NORMAL REGISTRATION SCREEN
   * ============================================================
   */
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
        {/* Header */}
        <Box sx={{ textAlign: "center", mb: 4 }}>
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
            {String(success)}
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

          {/* Password strength */}
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

          {/* Register */}
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

        <Stack direction="row" sx={{ justifyContent: "center", mt: 4 }} spacing={1}>
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

        <Typography mt={4} align="center" color="text.secondary" fontSize={13}>
          Fast • Secure • Trusted Payments
        </Typography>

        <Typography align="center" color="text.secondary" fontSize={12}>
          © {new Date().getFullYear()} CashTran. All Rights Reserved.
        </Typography>
      </Paper>
    </Box>
  );
}
