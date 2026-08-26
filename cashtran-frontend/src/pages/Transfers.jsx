import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import {
  Box,
  Container,
  Paper,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  CircularProgress,
  Alert,
  TextField,
  InputAdornment,
  IconButton,
  Grid,
  Card,
  CardContent,
  Stack,
  Button,
} from "@mui/material";

import {
  Search,
  Refresh,
  History,
  CheckCircle,
  PendingActions,
  ReceiptLong,
  ArrowBack,
} from "@mui/icons-material";

import { downloadTransactionHistory, getTransfers } from "../api/authApi";

export default function TransfersPage() {
  const navigate = useNavigate();
  const [transfers, setTransfers] = useState([]);
  const [filteredTransfers, setFilteredTransfers] = useState([]);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  async function loadTransfers() {
    try {
      setLoading(true);
      setError(null);
      const response = await getTransfers();
      setTransfers(response.data);
      setFilteredTransfers(response.data);
    } catch (err) {
      console.error(err);
      setError(err?.response?.data || "Unable to load transfers");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    (async () => {
      await loadTransfers();
    })();
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

  function handleSearch(value) {
    setSearch(value);
    const result = transfers.filter(
      (t) =>
        String(t.transferId).includes(value) ||
        t.transferStatusDesc?.toLowerCase().includes(value.toLowerCase()) ||
        String(t.accountTo).includes(value),
    );

    setFilteredTransfers(result);
  }

  const completedCount = transfers.filter((t) =>
    t.transferStatusDesc?.toLowerCase().includes("completed"),
  ).length;

  const pendingCount = transfers.filter((t) =>
    t.transferStatusDesc?.toLowerCase().includes("pending"),
  ).length;

  async function handleDownloadTransactionHistory() {
    try {
      const response = await downloadTransactionHistory();
      console.log(response.data);

      const url = window.URL.createObjectURL(new Blob([response.data]));

      const link = document.createElement("a");

      link.href = url;
      link.download = "CashTran_Transaction_History.pdf";

      document.body.appendChild(link);
      link.click();

      link.remove();

      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error(err);
      setError("Unable to download statement");
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
      <Container maxWidth="lg">
        <Button
          startIcon={<ArrowBack />}
          onClick={() => navigate("/dashboard")}
          sx={{ mb: 3 }}
        >
          Dashboard
        </Button>

        <Paper
          elevation={4}
          sx={{
            p: 4,
            borderRadius: 4,
            mb: 4,
          }}
        >
          <Stack direction="row" alignItems="center" spacing={2}>
            <History
              color="primary"
              sx={{
                fontSize: 45,
              }}
            />
            <Box>
              <Typography variant="h4" fontWeight="bold">
                Transfer History
              </Typography>
              <Typography color="text.secondary">
                View all CashTran transactions
              </Typography>
              <Box display="flex" justifyContent="flex-end" mb={3}>
                <Button
                  variant="contained"
                  startIcon={<ReceiptLong />}
                  onClick={handleDownloadTransactionHistory}
                >
                  Download Statement
                </Button>
              </Box>
            </Box>
          </Stack>
        </Paper>

        {error && (
          <Alert severity="error" sx={{ mb: 3 }}>
            {String(error)}
          </Alert>
        )}

        {/* Summary Cards */}
        <Grid container spacing={3} mb={4}>
          <Grid item xs={12} md={4}>
            <SummaryCard
              title="Total Transfers"
              value={transfers.length}
              icon={<ReceiptLong />}
            />
          </Grid>
          <Grid item xs={12} md={4}>
            <SummaryCard
              title="Completed"
              value={completedCount}
              icon={<CheckCircle />}
            />
          </Grid>

          <Grid item xs={12} md={4}>
            <SummaryCard
              title="Pending"
              value={pendingCount}
              icon={<PendingActions />}
            />
          </Grid>
        </Grid>
        <Paper
          sx={{
            p: 3,
            borderRadius: 4,
          }}
        >
          <Stack direction="row" spacing={2} mb={3}>
            <TextField
              fullWidth
              placeholder="Search transfers..."
              value={search}
              onChange={(e) => handleSearch(e.target.value)}
              InputAdornmentProps
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <Search />
                  </InputAdornment>
                ),
              }}
            />
            <IconButton onClick={loadTransfers}>
              <Refresh />
            </IconButton>
          </Stack>
          {loading ? (
            <Box display="flex" justifyContent="center" p={5}>
              <CircularProgress />
            </Box>
          ) : filteredTransfers.length === 0 ? (
            <Typography textAlign="center" p={4}>
              No transfers found.
            </Typography>
          ) : (
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>ID</TableCell>

                    <TableCell>Recipient</TableCell>

                    <TableCell>Amount</TableCell>

                    <TableCell>Status</TableCell>

                    <TableCell>Details</TableCell>
                  </TableRow>
                </TableHead>

                <TableBody>
                  {filteredTransfers.map((t) => (
                    <TableRow key={t.transferId} hover>
                      <TableCell>#{t.transferId}</TableCell>
                      <TableCell>{t.accountTo}</TableCell>

                      <TableCell>{formatCurrency(t.amount)}</TableCell>

                      <TableCell>
                        <Chip
                          label={t.transferStatusDesc}
                          color={statusColor(t.transferStatusDesc)}
                        />
                      </TableCell>

                      <TableCell>
                        <Button
                          size="small"
                          variant="outlined"
                          onClick={() => navigate(`/transfers/${t.transferId}`)}
                        >
                          View
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </Paper>
      </Container>
    </Box>
  );
}

function SummaryCard({ title, value, icon }) {
  return (
    <Card
      sx={{
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

            <Typography variant="h4" fontWeight="bold">
              {value}
            </Typography>
          </Box>

          <Box color="primary.main">{icon}</Box>
        </Stack>
      </CardContent>
    </Card>
  );
}
