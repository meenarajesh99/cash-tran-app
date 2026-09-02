import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import {
  Box,
  Container,
  Typography,
  Card,
  CardContent,
  Avatar,
  Grid,
  TextField,
  Alert,
  CircularProgress,
  InputAdornment,
  Paper,
  Button,
} from "@mui/material";

import SearchIcon from "@mui/icons-material/Search";
import PeopleIcon from "@mui/icons-material/People";
import ArrowBack from "@mui/icons-material/ArrowBack";
import { getUsers } from "../api/authApi";

export default function UsersPage() {
  const navigate = useNavigate();
  const [users, setUsers] = useState([]);
  const [filteredUsers, setFilteredUsers] = useState([]);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  async function loadUsers() {
    try {
      setLoading(true);
      const response = await getUsers();
      console.log("API users:", response.data);
      setUsers(response.data);
      setFilteredUsers(response.data);
    } catch (err) {
      console.error(err);
      setError(err?.response?.data || "Unable to load users");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    (async () => {
      await loadUsers();
    })();
  }, []);

  function handleSearch(value) {
    setSearch(value);

    const filtered = users.filter((user) =>
      user.username.toLowerCase().includes(value.toLowerCase()),
    );
    setFilteredUsers(filtered);
  }

  return (
    <Box
      sx={{
        minHeight: "100vh",
        background: "linear-gradient(135deg,#e3f2fd,#ffffff)",
        py: 4,
      }}
    >
      <Container maxWidth="lg">
        <Button
          startIcon={<ArrowBack />}
          onClick={() => navigate("/dashboard")}
          sx={{
            mb: 3,
          }}
        >
          Back to Dashboard
        </Button>
        {/* Header */}

        <Paper
          elevation={3}
          sx={{
            p: 3,
            mb: 4,
            borderRadius: 4,
          }}
        >
          <Box sx={{ display: "flex", alignItems: "center", gap: 2 }}>
            <PeopleIcon
              color="primary"
              sx={{
                fontSize: 45,
              }}
            />
            <Box>
              <Typography variant="h4" fontWeight="bold">
                Users
              </Typography>
              <Typography color="text.secondary">
                Find CashTran users and send money securely
              </Typography>
            </Box>
          </Box>
        </Paper>

        {error && (
          <Alert severity="error" sx={{ mb: 3 }}>
            {String(error)}
          </Alert>
        )}

        {/* Search */}
        <TextField
          fullWidth
          placeholder="Search users..."
          value={search}
          onChange={(e) => handleSearch(e.target.value)}
          sx={{
            mb: 4,
            backgroundColor: "white",
            borderRadius: 2,
          }}

          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon />
              </InputAdornment>
            ),
          }}
        />

        {loading ? (
          <Box sx={{ display: "flex", justifyContent: "center", mt: 5 }}>
            <CircularProgress size={60} />
          </Box>
        ) : filteredUsers.length === 0 ? (
          <Paper
            sx={{
              p: 5,
              textAlign: "center",
              borderRadius: 4,
            }}
          >
            <Typography>No users found.</Typography>
          </Paper>
        ) : (
          <Grid container spacing={3}>
            {filteredUsers.map((user) => (
              <Grid item xs={12} sm={6} md={4} key={user.id}>
                <Card
                  onClick={() => navigate(`/transfer/send?user=${user.id}`)}
                  sx={{
                    borderRadius: 4,
                    transition: "0.3s",
                    "&:hover": {
                      transform: "translateY(-5px)",
                      boxShadow: 6,
                    },
                  }}
                >
                  <CardContent>
                    <Box sx={{ display: "flex", alignItems: "center", gap: 2 }}>
                      <Avatar
                        sx={{
                          width: 55,
                          height: 55,
                          bgcolor: "primary.main",
                          fontSize: 22,
                        }}
                      >
                        {user.username?.charAt(0)?.toUpperCase()}
                      </Avatar>
                      <Box>
                        <Typography variant="h6" fontWeight="bold">
                          {user.username}
                        </Typography>
                        <Typography color="text.secondary">
                          User ID: {user.id}
                        </Typography>
                      </Box>
                    </Box>
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>
        )}
      </Container>
    </Box>
  );
}
