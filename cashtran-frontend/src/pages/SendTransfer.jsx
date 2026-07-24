import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import {
  Box,
  Container,
  Paper,
  Typography,
  TextField,
  Button,
  MenuItem,
  Avatar,
  Alert,
  CircularProgress,
  InputAdornment,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Divider,
} from "@mui/material";

import { Send, AccountBalanceWallet, ArrowBack } from "@mui/icons-material";
import { AuthContext } from "../auth/AuthProvider";

import { getUsers, sendTransfer } from "../api/authApi";

export default function SendTransfer() {
  const navigate = useNavigate();

  const [users, setUsers] = useState([]);

  const [toUser, setToUser] = useState("");

  const [amount, setAmount] = useState("");

  const [message, setMessage] = useState(null);

  const [loading, setLoading] = useState(false);

  const [openConfirm, setOpenConfirm] = useState(false);
  const { user: loggedInUser } = React.useContext(AuthContext);

  async function loadUsers() {
    try {
      const response = await getUsers();
      console.log("Logged in user:", loggedInUser);
      console.log("Logged in user id:", loggedInUser?.id);
      console.log("Users from backend:", response.data);

      const otherUsers = response.data.filter(
        (user) => Number(user.id) !== loggedInUser?.id,
      );

      setUsers(otherUsers);
    } catch (err) {
      console.error(err);

      setMessage({
        type: "error",
        text: "Unable to load users",
      });
    }
  }

  useEffect(() => {
    if (loggedInUser?.id) {
      loadUsers();
    }
  }, [loggedInUser]);

  const selectedUser = users.find((user) => Number(user.id) === Number(toUser));

  function validateAmount() {
    const value = Number(amount);

    if (isNaN(value) || value <= 0) {
      setMessage({
        type: "error",
        text: "Amount must be greater than zero",
      });

      return false;
    }
    return true;
  }

  async function confirmTransfer() {
    setOpenConfirm(false);

    if (!validateAmount()) {
      return;
    }

    try {
      setLoading(true);
      setMessage(null);
      const response = await sendTransfer(Number(toUser), Number(amount));
      setMessage({
        type: "success",
        text: `Transfer successful. Transaction ID: ${response.data.transferId}`,
      });

      setAmount("");
      setToUser("");
    } catch (err) {
      setMessage({
        type: "error",

        text: err?.response?.data || err.message || "Transfer failed",
      });
    } finally {
      setLoading(false);
    }
  }

  function submitForm(e) {
    e.preventDefault();
    if (!toUser) {
      setMessage({
        type: "error",

        text: "Please select a recipient",
      });
      return;
    }

    if (!amount) {
      setMessage({
        type: "error",

        text: "Please enter an amount",
      });
      return;
    }

    setOpenConfirm(true);
  }

  return (
    <Box
      sx={{
        minHeight: "100vh",
        background: "linear-gradient(135deg,#e3f2fd,#ffffff)",
        py: 5,
      }}
    >
      <Container maxWidth="sm">
        <Button
          startIcon={<ArrowBack />}
          onClick={() => navigate("/dashboard")}
          sx={{
            mb: 3,
          }}
        >
          Back to Dashboard
        </Button>

        <Paper
          elevation={5}
          sx={{
            p: 4,
            borderRadius: 5,
          }}
        >
          <Box textAlign="center" mb={4}>
            <AccountBalanceWallet
              sx={{
                fontSize: 65,

                color: "primary.main",
              }}
            />

            <Typography variant="h4" fontWeight="bold">
              Send Money
            </Typography>

            <Typography color="text.secondary">
              Transfer money securely with CashTran
            </Typography>
          </Box>

          {message && (
            <Alert
              severity={message.type}
              sx={{
                mb: 3,
              }}
            >
              {message.text}
            </Alert>
          )}

          <Box component="form" onSubmit={submitForm}>
            <TextField
              select
              fullWidth
              label="Select Recipient"
              value={toUser}
              onChange={(e) => setToUser(e.target.value)}
              margin="normal"
            >
              {users.map((user) => (
                <MenuItem key={user.id} value={String(user.id)}>
                  <Box display="flex" alignItems="center" gap={2}>
                    <Avatar>{user.username?.charAt(0)?.toUpperCase()}</Avatar>
                    {user.username}
                  </Box>
                </MenuItem>
              ))}
            </TextField>

            <TextField
              fullWidth
              label="Amount"
              type="number"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              margin="normal"
              inputProps={{
                min: 0.01,
                step: 0.01,
              }}

              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">$</InputAdornment>
                ),
              }}
            />

            {selectedUser && (
              <Paper
                variant="outlined"

                sx={{
                  mt: 3,
                  p: 2,
                  borderRadius: 3,
                }}
              >
                <Typography>Sending money to:</Typography>
                <Divider sx={{ my: 1 }} />
                <Box display="flex" alignItems="center" gap={2}>
                  <Avatar>
                    {selectedUser.username?.charAt(0)?.toUpperCase()}
                  </Avatar>
                  <Typography fontWeight="bold">
                    {selectedUser.username}
                  </Typography>
                </Box>
              </Paper>
            )}

            <Button
              fullWidth
              type="submit"
              variant="contained"
              size="large"
              startIcon={<Send />}
              disabled={loading}
              sx={{
                mt: 4,
                py: 1.5,
                borderRadius: 3,
                fontWeight: "bold",
              }}
            >
              {loading ? (
                <CircularProgress size={26} color="inherit" />
              ) : (
                "Send Money"
              )}
            </Button>
          </Box>
        </Paper>
      </Container>

      {/* Confirmation Dialog */}

      <Dialog open={openConfirm} onClose={() => setOpenConfirm(false)}>
        <DialogTitle>Confirm Transfer</DialogTitle>
        <DialogContent>
          <Typography>
            Send <strong>${amount}</strong> to{" "}
            <strong>{selectedUser?.username}</strong>?
          </Typography>
        </DialogContent>

        <DialogActions>
          <Button onClick={() => setOpenConfirm(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={confirmTransfer}
            disabled={loading}
          >
            {loading ? <CircularProgress size={24} /> : "Confirm"}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
