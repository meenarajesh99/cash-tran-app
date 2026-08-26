import { useContext, useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { AuthContext } from "../auth/AuthProvider";

import {
  Box,
  Paper,
  Typography,
  TextField,
  Button,
  Alert,
  Checkbox,
  FormControlLabel,
  IconButton,
  InputAdornment,
  Link,
  Divider,
} from "@mui/material";

import PersonIcon from "@mui/icons-material/Person";
import LockIcon from "@mui/icons-material/Lock";
import AccountBalanceWalletIcon from "@mui/icons-material/AccountBalanceWallet";
import Visibility from "@mui/icons-material/Visibility";
import VisibilityOff from "@mui/icons-material/VisibilityOff";
import { useForm, Controller } from "react-hook-form";

import CircularProgress from "@mui/material/CircularProgress";

export default function LoginPage() {
  const { login } = useContext(AuthContext);

  const navigate = useNavigate();

  const [loading, setLoading] = useState(false);

  const {
    control,
    handleSubmit,
    setValue,
    formState: { errors },
  } = useForm({
    defaultValues: {
      username: "",
      password: "",
    },
  });
  const [rememberMe, setRememberMe] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [darkMode] = useState(false);
  const [error, setError] = useState(null);

  async function onSubmit(data) {
    setLoading(true);
    setError(null);
    try {
      await login(data.username, data.password);
      if (rememberMe) {
        localStorage.setItem("rememberedUser", data.username);
      } else {
        localStorage.removeItem("rememberedUser");
      }
      navigate("/dashboard");
    } catch (err) {
      setError(err?.response?.data || err.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    (async () => {
      const remembered = localStorage.getItem("rememberedUser");
      if (remembered) {
        setValue("username", remembered);
        setRememberMe(true);
      }
    })();
  }, [setValue]);

  return (
    <Box
      sx={{
        minHeight: "100vh",
        background: darkMode
          ? "linear-gradient(135deg,#0f172a,#1e293b)"
          : "linear-gradient(135deg,#1565c0,#42a5f5)",
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
        {/* Logo */}
        <Box textAlign="center" mb={4}>
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
            Secure Digital Payments
          </Typography>
        </Box>

        {error && (
          <Alert severity="error" sx={{ mb: 3 }}>
            {String(error)}
          </Alert>
        )}

        <Box component="form" onSubmit={handleSubmit(onSubmit)}>
          <Controller
            name="username"
            control={control}
            rules={{
              required: "Username is required",
              minLength: {
                value: 3,
                message: "Minimum 3 characters",
              },
            }}
            render={({ field }) => (
              <TextField
                {...field}
                fullWidth
                margin="normal"
                label="Username"
                error={!!errors.username}
                helperText={errors.username?.message}
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <PersonIcon />
                    </InputAdornment>
                  ),
                }}
              />
            )}
          />

          <Controller
            name="password"
            control={control}
            rules={{
              required: "Password is required",
              minLength: {
                value: 6,
                message: "Minimum 6 characters",
              },
            }}
            render={({ field }) => (
              <TextField
                {...field}
                fullWidth
                margin="normal"
                label="Password"
                type={showPassword ? "text" : "password"}
                error={!!errors.password}
                helperText={errors.password?.message}
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <LockIcon />
                    </InputAdornment>
                  ),
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton
                        onClick={() => setShowPassword(!showPassword)}
                      >
                        {showPassword ? <VisibilityOff /> : <Visibility />}
                      </IconButton>
                    </InputAdornment>
                  ),
                }}
              />
            )}
          />

          <Box
            display="flex"
            justifyContent="space-between"
            alignItems="center"
            mt={1}
          >
            <FormControlLabel
              control={
                <Checkbox
                  checked={rememberMe}
                  onChange={(e) => setRememberMe(e.target.checked)}
                />
              }
              label="Remember Me"
            />

            <Link
              component="button"
              underline="hover"
              onClick={() => navigate("/forgot-password")}
            >
              Forgot Password?
            </Link>
          </Box>

          <Button
            fullWidth
            variant="contained"
            size="large"
            type="submit"
            disabled={loading}
            sx={{
              mt: 3,
              py: 1.5,
              borderRadius: 3,
              fontWeight: "bold",
            }}
          >
            {loading ? <CircularProgress color="inherit" size={28} /> : "Login"}
          </Button>

          <Divider sx={{ my: 4 }}>OR</Divider>

          <Button
            fullWidth
            variant="outlined"
            size="large"
            sx={{
              borderRadius: 3,
              py: 1.3,
              fontWeight: "bold",
            }}
            onClick={() => navigate("/register")}
          >
            Create New Account
          </Button>
        </Box>

        <Typography
          mt={4}
          textAlign="center"
          color="text.secondary"
          fontSize={13}
        >
          Fast • Secure • Trusted Payments
        </Typography>

        <Typography textAlign="center" color="text.secondary" fontSize={12}>
          © {new Date().getFullYear()} CashTran. All Rights Reserved.
        </Typography>
      </Paper>
    </Box>
  );
}
