import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import { getUsers, createMoneyRequest } from "../api/authApi";
import { AuthContext } from "../auth/AuthProvider";

import {
  Container,
  Paper,
  Typography,
  TextField,
  Button,
  Box,
  Alert,
  CircularProgress,
  MenuItem,
} from "@mui/material";

function RequestMoney() {
  const navigate = useNavigate();

  const { user: loggedInUser } = React.useContext(AuthContext);

  const [users, setUsers] = useState([]);

  const [userId, setUserId] = useState("");

  const [amount, setAmount] = useState("");

  const [message, setMessage] = useState("");

  const [error, setError] = useState("");

  const [loading, setLoading] = useState(false);

  // Load users except logged-in user
  useEffect(() => {
    async function loadUsers() {
      try {
        const response = await getUsers();

        const otherUsers = response.data.filter(
          (user) => Number(user.id) !== Number(loggedInUser?.id),
        );

        setUsers(otherUsers);
      } catch (err) {
        console.error("Unable to load users:", err);

        setError("Unable to load users");
      }
    }

    if (loggedInUser?.id) {
      loadUsers();
    }
  }, [loggedInUser]);

  async function handleSubmit(event) {
    event.preventDefault();

    setMessage("");
    setError("");

    if (!userId) {
      setError("Please select a user");
      return;
    }

    if (!amount || Number(amount) <= 0) {
      setError("Amount must be greater than zero");
      return;
    }

    try {
      setLoading(true);

      await createMoneyRequest(Number(userId), Number(amount));

      setMessage("Money request created successfully!");

      setUserId("");
      setAmount("");
    } catch (err) {
      console.error("Money request error:", err);

      setError(
        err.response?.data?.message ||
          err.response?.data ||
          "Unable to create money request",
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <Container maxWidth="sm">
      <Paper
        elevation={3}
        sx={{
          mt: 5,
          p: 4,
        }}
      >
        <Typography variant="h5" mb={2}>
          Request Money
        </Typography>

        {message && (
          <Alert severity="success" sx={{ mb: 2 }}>
            {message}
          </Alert>
        )}

        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        <Box
          component="form"
          onSubmit={handleSubmit}
          sx={{
            display: "flex",
            flexDirection: "column",
            gap: 2,
          }}
        >
          <TextField
            select
            label="Select User"
            value={userId}
            onChange={(e) => setUserId(e.target.value)}
            required
          >
            {users.map((user) => (
              <MenuItem key={user.id} value={user.id}>
                {user.username}
              </MenuItem>
            ))}
          </TextField>

          <TextField
            label="Amount"
            type="number"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            inputProps={{
              min: 0.01,
              step: 0.01,
            }}
            required
          />

          <Button variant="contained" type="submit" disabled={loading}>
            {loading ? (
              <CircularProgress size={24} color="inherit" />
            ) : (
              "Request Money"
            )}
          </Button>

          <Button variant="outlined" onClick={() => navigate("/dashboard")}>
            Back to Dashboard
          </Button>
        </Box>
      </Paper>
    </Container>
  );
}

export default RequestMoney;
