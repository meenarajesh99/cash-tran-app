import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import {
  AppBar,
  Toolbar,
  Typography,
  Box,
  Container,
  Card,
  CardContent,
  Grid,
  Button,
  Avatar,
  IconButton,
  Chip,
  Alert,
  CircularProgress,
  Divider,
  Paper,
  Stack,
  Tooltip,
} from "@mui/material";

import {
  AccountBalanceWallet,
  Logout,
  Send,
  People,
  History,
  RequestPage,
  Person,
  DarkMode,
  LightMode,
  TrendingUp,
  PendingActions,
  CheckCircle,
  Refresh,
} from "@mui/icons-material";

import {
  getBalance,
  getTransfers,
  getPendingSentTransfers,
  getPendingReceivedTransfers,
  approveTransfer,
  rejectTransfer,
} from "../api/authApi";

import { AuthContext } from "../auth/AuthProvider";

export default function Dashboard() {
  const { user, logout } = React.useContext(AuthContext);

  const navigate = useNavigate();

  const [balance, setBalance] = useState(null);
  const [transfers, setTransfers] = useState([]);
  const [pendingReceived, setPendingReceived] = useState([]);
  const [pendingSent, setPendingSent] = useState([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [darkMode, setDarkMode] = useState(false);

  async function loadDashboard() {
    setLoading(true);
    setError(null);

    try {
      const [
        balanceResponse,
        transferResponse,
        sentResponse,
        receivedResponse,
      ] = await Promise.all([
        getBalance(),
        getTransfers(),
        getPendingSentTransfers(),
        getPendingReceivedTransfers(),
      ]);

      setBalance(balanceResponse.data);

      setTransfers(transferResponse.data);

      setPendingSent(sentResponse.data);

      setPendingReceived(receivedResponse.data);
    } catch (err) {
      console.error(err);

      setError(err?.response?.data || "Unable to load dashboard information");
    } finally {
      setLoading(false);
    }
  }

  async function handleApprove(transferId) {
    try {
      await approveTransfer(transferId);

      await loadDashboard();
    } catch (err) {
      console.error(err);

      setError(err?.response?.data || "Unable to approve transfer");
    }
  }

  async function handleReject(transferId) {
    try {
      await rejectTransfer(transferId);

      await loadDashboard();
    } catch (err) {
      console.error(err);

      setError(err?.response?.data || "Unable to reject transfer");
    }
  }

  useEffect(() => {
    loadDashboard();
  }, []);

  function formatCurrency(amount) {
    return new Intl.NumberFormat("en-US", {
      style: "currency",
      currency: "USD",
    }).format(amount || 0);
  }

  function statusColor(status) {
    if (!status) return "default";

    const value = status.toLowerCase();

    if (value.includes("approved")) return "success";

    if (value.includes("pending")) return "warning";

    if (value.includes("rejected")) return "error";

    return "default";
  }

  const approvedCount = transfers.filter((t) =>
    t.transferStatusDesc?.toLowerCase().includes("approved"),
  ).length;

  const pendingCount = pendingReceived.length + pendingSent.length;

  const allTransfers = [
    ...transfers,
    ...pendingSent.filter(
      (p) => !transfers.some((t) => t.transferId === p.transferId),
    ),
    ...pendingReceived.filter(
      (p) => !transfers.some((t) => t.transferId === p.transferId),
    ),
  ];

  return (
    <Box
      sx={{
        minHeight: "100vh",

        background: darkMode
          ? "linear-gradient(135deg,#111827,#1f2937)"
          : "linear-gradient(135deg,#e3f2fd,#ffffff)",
      }}
    >
      <AppBar position="static">
        <Toolbar>
          <AccountBalanceWallet
            sx={{
              mr: 1,
            }}
          />

          <Typography
            variant="h6"
            sx={{
              flexGrow: 1,
              fontWeight: "bold",
            }}
          >
            CashTran
          </Typography>

          <Tooltip title="Toggle Theme">
            <IconButton color="inherit" onClick={() => setDarkMode(!darkMode)}>
              {darkMode ? <LightMode /> : <DarkMode />}
            </IconButton>
          </Tooltip>

          <Button color="inherit" startIcon={<Logout />} onClick={logout}>
            Logout
          </Button>
        </Toolbar>
      </AppBar>

      <Container
        maxWidth="lg"
        sx={{
          mt: 4,
          mb: 5,
        }}
      >
        <Box display="flex" alignItems="center" mb={4}>
          <Avatar
            sx={{
              width: 60,
              height: 60,
              mr: 2,
              bgcolor: "primary.main",
            }}
          >
            {user?.username?.charAt(0)?.toUpperCase()}
          </Avatar>

          <Box>
            <Typography variant="h4" fontWeight="bold">
              Welcome back, {user?.username}
            </Typography>

            <Typography color="text.secondary">
              Manage your money securely with CashTran
            </Typography>
          </Box>
        </Box>

        {error && (
          <Alert
            severity="error"
            sx={{
              mb: 3,
            }}
          >
            {String(error)}
          </Alert>
        )}

        {loading ? (
          <Box display="flex" justifyContent="center" mt={8}>
            <CircularProgress size={60} />
          </Box>
        ) : (
          <>
            {" "}
            <Grid container spacing={3}>
              <Grid item xs={12} md={3}>
                <StatCard
                  title="Balance"
                  value={formatCurrency(balance)}
                  icon={<AccountBalanceWallet />}
                />
              </Grid>

              <Grid item xs={12} md={3}>
                <StatCard
                  title="Approved"
                  value={approvedCount}
                  icon={<CheckCircle />}
                />
              </Grid>

              <Grid item xs={12} md={3}>
                <StatCard
                  title="Pending"
                  value={pendingCount}
                  icon={<PendingActions />}
                />
              </Grid>

              <Grid item xs={12} md={3}>
                <StatCard
                  title="Transfers"
                  value={transfers.length}
                  icon={<TrendingUp />}
                />
              </Grid>
            </Grid>
            <Paper
              sx={{
                mt: 4,
                p: 3,
                borderRadius: 4,
              }}
            >
              <Typography variant="h6" fontWeight="bold" mb={2}>
                Quick Actions
              </Typography>

              <Stack
                direction={{
                  xs: "column",
                  md: "row",
                }}
                spacing={2}
              >
                <ActionButton
                  text="My Account"
                  icon={<Person />}
                  action={() => navigate("/account")}
                />
                <ActionButton
                  text="Send Money"
                  icon={<Send />}
                  action={() => navigate("/transfer/send")}
                />

                <ActionButton
                  text="Users"
                  icon={<People />}
                  action={() => navigate("/users")}
                />

                <ActionButton
                  text="History"
                  icon={<History />}
                  action={() => navigate("/transfers")}
                />

                <ActionButton
                  text="Request Money"
                  icon={<RequestPage />}
                  action={() => navigate("/request-money")}
                />
              </Stack>
            </Paper>
            <Paper
              sx={{
                mt: 4,
                p: 3,
                borderRadius: 4,
              }}
            >
              <Box
                display="flex"
                justifyContent="space-between"
                alignItems="center"
              >
                <Typography variant="h6" fontWeight="bold">
                  Recent Transfers
                </Typography>

                <IconButton onClick={loadDashboard}>
                  <Refresh />
                </IconButton>
              </Box>

              <Divider
                sx={{
                  my: 2,
                }}
              />

              {allTransfers.length === 0 ? (
                <Typography>No transfers found.</Typography>
              ) : (
                allTransfers.map((transfer) => (
                  <Card
                    key={transfer.transferId}
                    sx={{
                      mb: 2,
                      "&:hover": {
                        boxShadow: 5,
                      },
                    }}
                  >
                    <CardContent>
                      <Stack
                        direction="row"
                        justifyContent="space-between"
                        alignItems="center"
                      >
                        <Box>
                          <Typography fontWeight="bold">
                            Transfer #{transfer.transferId}
                          </Typography>

                          <Box>
                            {transfer.transferTypeDesc === "Request" ? (
                              <Typography>
                                {transfer.accountFromUsername === user?.username
                                  ? `You requested money from ${transfer.accountToUsername}`
                                  : `${transfer.accountFromUsername} requested money from you`}
                              </Typography>
                            ) : (
                              <Typography>
                                {transfer.accountFromUsername === user?.username
                                  ? `Sent to ${transfer.accountToUsername}`
                                  : `Received from ${transfer.accountFromUsername}`}
                              </Typography>
                            )}

                            <Typography>
                              Amount: {formatCurrency(transfer.amount)}
                            </Typography>
                          </Box>
                        </Box>

                        <Box>
                          {pendingReceived.some(
                            (item) => item.transferId === transfer.transferId,
                          ) && (
                            <Box sx={{ mb: 1 }}>
                              <Button
                                size="small"
                                variant="contained"
                                color="success"
                                onClick={() =>
                                  handleApprove(transfer.transferId)
                                }
                                sx={{
                                  mr: 1,
                                }}
                              >
                                Approve
                              </Button>

                              <Button
                                size="small"
                                variant="outlined"
                                color="error"
                                onClick={() =>
                                  handleReject(transfer.transferId)
                                }
                              >
                                Reject
                              </Button>
                            </Box>
                          )}

                          <Chip
                            label={transfer.transferStatusDesc}
                            color={statusColor(transfer.transferStatusDesc)}
                          />
                        </Box>
                      </Stack>
                    </CardContent>
                  </Card>
                ))
              )}
            </Paper>
          </>
        )}
      </Container>
    </Box>
  );
}

function StatCard({ title, value, icon }) {
  return (
    <Card
      sx={{
        height: "100%",
        borderRadius: 4,
      }}
    >
      <CardContent>
        <Stack
          direction="row"
          justifyContent="space-between"
          alignItems="center"
        >
          <Box>
            <Typography color="text.secondary">{title}</Typography>

            <Typography variant="h5" fontWeight="bold">
              {value}
            </Typography>
          </Box>

          <Box color="primary.main">{icon}</Box>
        </Stack>
      </CardContent>
    </Card>
  );
}

function ActionButton({ text, icon, action }) {
  return (
    <Button
      variant="contained"

      startIcon={icon}

      onClick={action}

      sx={{
        flex: 1,
        py: 1.5,
        borderRadius: 3,
        fontWeight: "bold",
      }}
    >
      {text}
    </Button>
  );
}
