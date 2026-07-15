const API_BASE_URL = 'http://localhost:8080/';
let currentUser = null;
let authToken = null;

// Auth Functions
function switchTab(tab) {
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    document.querySelectorAll('.auth-form').forEach(form => form.classList.remove('active'));

    event.target.classList.add('active');
    document.getElementById(`${tab}-form`).classList.add('active');
}

document.getElementById('login-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const username = document.getElementById('login-username').value;
    const password = document.getElementById('login-password').value;

    try {
        const response = await fetch(API_BASE_URL + 'login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        if (!response.ok) throw new Error('Login failed');

        const data = await response.json();
        authToken = data.token;
        currentUser = data.user;

        showDashboard();
    } catch (error) {
        showError('login-error', error.message);
    }
});

document.getElementById('register-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const username = document.getElementById('register-username').value;
    const password = document.getElementById('register-password').value;
    const confirm = document.getElementById('register-confirm').value;

    if (password !== confirm) {
        showError('register-error', 'Passwords do not match');
        return;
    }

    try {
        const response = await fetch(API_BASE_URL + 'register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        if (!response.ok) throw new Error('Registration failed');

        switchTab('login');
        showSuccess('register-error', 'Registration successful! Please login.');
    } catch (error) {
        showError('register-error', error.message);
    }
});

// Dashboard Functions
function showDashboard() {
    document.getElementById('auth-page').classList.remove('active');
    document.getElementById('dashboard-page').classList.add('active');
    document.getElementById('current-user').textContent = `Welcome, ${currentUser.username}!`;

    loadUsers();
    loadBalance();
    loadTransfers();
    loadPending();
}

function logout() {
    authToken = null;
    currentUser = null;
    document.getElementById('dashboard-page').classList.remove('active');
    document.getElementById('auth-page').classList.add('active');
    document.getElementById('login-form').reset();
    document.getElementById('register-form').reset();
}

function showSection(sectionId) {
    document.querySelectorAll('.section').forEach(s => s.classList.remove('active'));
    document.querySelectorAll('.menu-item').forEach(m => m.classList.remove('active'));

    document.getElementById(sectionId).classList.add('active');
    event.target.classList.add('active');
}

async function loadBalance() {
    try {
        const response = await fetch(API_BASE_URL + 'balance', {
            headers: { 'Authorization': `Bearer ${authToken}` }
        });
        const balance = await response.json();
        document.getElementById('balance-amount').textContent = `$${balance.toFixed(2)}`;
    } catch (error) {
        console.error('Error loading balance:', error);
    }
}

function refreshBalance() {
    loadBalance();
    showSuccess('balance', 'Balance refreshed!');
}

async function loadTransfers() {
    try {
        const response = await fetch(API_BASE_URL + 'transfers', {
            headers: { 'Authorization': `Bearer ${authToken}` }
        });
        const transfers = await response.json();

        const tbody = document.getElementById('transfers-tbody');
        tbody.innerHTML = transfers.map(t => `
            <tr>
                <td>${t.transferId}</td>
                <td>${t.accountFrom === currentUser.id ? 'To: ' : 'From: '} ${t.accountTo === currentUser.id ? t.accountFrom : t.accountTo}</td>
                <td>$${t.amount.toFixed(2)}</td>
                <td>${t.transferTypeDesc}</td>
                <td><span class="status-badge status-${t.transferStatusDesc.toLowerCase()}">${t.transferStatusDesc}</span></td>
                <td><button class="btn btn-secondary" onclick="viewTransferDetails(${t.transferId})">View</button></td>
            </tr>
        `).join('');
    } catch (error) {
        console.error('Error loading transfers:', error);
    }
}

async function loadPending() {
    try {
        const response = await fetch(API_BASE_URL + 'transfers/pending', {
            headers: { 'Authorization': `Bearer ${authToken}` }
        });
        const pending = await response.json();

        const tbody = document.getElementById('pending-tbody');
        tbody.innerHTML = pending.map(t => `
            <tr>
                <td>${t.transferId}</td>
                <td>${t.accountFrom}</td>
                <td>$${t.amount.toFixed(2)}</td>
                <td>
                    <button class="btn btn-success" onclick="approvePending(${t.transferId}, ${t.amount})">Approve</button>
                    <button class="btn btn-error" onclick="rejectPending(${t.transferId}, ${t.amount})">Reject</button>
                </td>
            </tr>
        `).join('');
    } catch (error) {
        console.error('Error loading pending:', error);
    }
}

async function loadUsers() {
    try {
        const response = await fetch(API_BASE_URL + 'users', {
            headers: { 'Authorization': `Bearer ${authToken}` }
        });
        const users = await response.json();

        const sendSelect = document.getElementById('send-recipient');
        const requestSelect = document.getElementById('request-payer');

        const options = users.map(u => `<option value="${u.id}">${u.username}</option>`).join('');
        sendSelect.innerHTML = '<option value="">Choose a user...</option>' + options;
        requestSelect.innerHTML = '<option value="">Choose a user...</option>' + options;
    } catch (error) {
        console.error('Error loading users:', error);
    }
}

document.getElementById('send-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const recipientId = document.getElementById('send-recipient').value;
    const amount = parseFloat(document.getElementById('send-amount').value);

    try {
        const response = await fetch(API_BASE_URL + 'transfers', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${authToken}`
            },
            body: JSON.stringify({ accountTo: recipientId, amount })
        });

        if (!response.ok) throw new Error('Transfer failed');

        showSuccess('send-success', 'Transfer sent successfully!');
        document.getElementById('send-form').reset();
        loadTransfers();
    } catch (error) {
        showError('send-error', error.message);
    }
});

document.getElementById('request-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const payerId = document.getElementById('request-payer').value;
    const amount = parseFloat(document.getElementById('request-amount').value);

    try {
        const response = await fetch(API_BASE_URL + 'requests', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${authToken}`
            },
            body: JSON.stringify({ accountFrom: payerId, amount })
        });

        if (!response.ok) throw new Error('Request failed');

        showSuccess('request-success', 'Request sent successfully!');
        document.getElementById('request-form').reset();
    } catch (error) {
        showError('request-error', error.message);
    }
});

async function approvePending(transferId, amount) {
    try {
        const response = await fetch(API_BASE_URL + `transfer/${transferId}/accept`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${authToken}`
            },
            body: JSON.stringify({ amount })
        });

        if (!response.ok) throw new Error('Approval failed');

        loadPending();
        loadBalance();
        alert('Transfer approved!');
    } catch (error) {
        alert('Error: ' + error.message);
    }
}

async function rejectPending(transferId, amount) {
    try {
        const response = await fetch(API_BASE_URL + `transfer/${transferId}/reject`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${authToken}`
            },
            body: JSON.stringify({ amount })
        });

        if (!response.ok) throw new Error('Rejection failed');

        loadPending();
        alert('Transfer rejected!');
    } catch (error) {
        alert('Error: ' + error.message);
    }
}

function showError(elementId, message) {
    const elem = document.getElementById(elementId);
    elem.textContent = message;
    elem.classList.add('show');
    setTimeout(() => elem.classList.remove('show'), 5000);
}

function showSuccess(elementId, message) {
    const elem = document.getElementById(elementId);
    elem.textContent = message;
    elem.classList.add('show');
    setTimeout(() => elem.classList.remove('show'), 3000);
}