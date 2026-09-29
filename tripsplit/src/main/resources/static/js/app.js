const API = '/api';

function money(value) {
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(Number(value || 0));
}

async function request(url, options = {}) {
    const response = await fetch(url, options);
    const text = await response.text();
    let data = {};
    try { data = text ? JSON.parse(text) : {}; } catch (_) { data = { message: text }; }
    if (!response.ok) throw new Error(data.message || 'Request failed');
    return data;
}

function showMessage(id, message, success = false) {
    const box = document.getElementById(id);
    if (!box) return;
    box.textContent = message;
    box.classList.remove('hidden', 'success');
    if (success) box.classList.add('success');
}

function tripIdFromUrl() {
    const parts = window.location.pathname.split('/').filter(Boolean);
    return parts[1] || null;
}

function formatDate(date) {
    if (!date) return '-';
    return new Date(date + 'T00:00:00').toLocaleDateString('en-IN');
}

async function loadTrips() {
    const trips = await request(`${API}/trips`);
    const totalTrips = document.getElementById('totalTrips');
    const activeTrips = document.getElementById('activeTrips');
    if (totalTrips) totalTrips.textContent = trips.length;
    if (activeTrips) {
        const today = new Date().toISOString().slice(0, 10);
        activeTrips.textContent = trips.filter(t => t.startDate <= today && t.endDate >= today).length;
    }

    let expenseCount = 0, totalAmount = 0;
    for (const trip of trips) {
        const expenses = await request(`${API}/trips/${trip.id}/expenses`);
        expenseCount += expenses.length;
        totalAmount += expenses.reduce((sum, e) => sum + Number(e.amount), 0);
    }
    const expenseElement = document.getElementById('totalExpenses');
    const amountElement = document.getElementById('totalAmount');
    if (expenseElement) expenseElement.textContent = expenseCount;
    if (amountElement) amountElement.textContent = money(totalAmount);

    const recent = document.getElementById('recentTrips');
    if (recent) renderTrips(recent, trips.slice(-6).reverse());
    const list = document.getElementById('tripsList');
    if (list) renderTrips(list, trips);
}

function renderTrips(container, trips) {
    if (!trips.length) {
        container.innerHTML = '<p class="muted">No trips yet. Create your first trip.</p>';
        return;
    }
    container.innerHTML = trips.map(t => `
        <article class="trip-card">
            <h3>${escapeHtml(t.name)}</h3>
            <p>${escapeHtml(t.description || 'No description')}</p>
            <div class="trip-meta"><span>${formatDate(t.startDate)}</span><span>→</span><span>${formatDate(t.endDate)}</span></div>
            <div class="trip-actions"><a class="button small" href="/trips/${t.id}">View</a><button class="button small secondary" onclick="deleteTrip(${t.id})">Delete</button></div>
        </article>`).join('');
}

async function editTrip(id) {
    try {
        const trip = await request(`${API}/trips/${id}`);
        const name = prompt('Trip name:', trip.name);
        if (name === null) return;
        const description = prompt('Description:', trip.description || '');
        if (description === null) return;
        const startDate = prompt('Start date (YYYY-MM-DD):', trip.startDate);
        if (startDate === null) return;
        const endDate = prompt('End date (YYYY-MM-DD):', trip.endDate);
        if (endDate === null) return;
        await request(`${API}/trips/${id}`, {method:'PUT', headers:{'Content-Type':'application/json'}, body:JSON.stringify({name, description, startDate, endDate})});
        await loadTripDetails();
    } catch (e) { alert(e.message); }
}

async function editParticipant(participantId, tripId) {
    try {
        const people = await request(`${API}/trips/${tripId}/participants`);
        const person = people.find(p => p.id === participantId);
        if (!person) return;
        const name = prompt('Participant name:', person.name);
        if (name === null) return;
        const email = prompt('Email:', person.email || '');
        if (email === null) return;
        await request(`${API}/participants/${participantId}`, {method:'PUT', headers:{'Content-Type':'application/json'}, body:JSON.stringify({name, email})});
        await loadParticipants(tripId);
    } catch (e) { alert(e.message); }
}

async function editExpense(expenseId, tripId) {
    try {
        const expense = await request(`${API}/trips/${tripId}/expenses`);
        const current = expense.find(e => e.id === expenseId);
        const people = await request(`${API}/trips/${tripId}/participants`);
        if (!current) return;
        const description = prompt('Description:', current.description);
        if (description === null) return;
        const amount = prompt('Amount:', current.amount);
        if (amount === null) return;
        const payerName = prompt(`Payer name (${people.map(p => p.name).join(', ')}):`, current.payer.name);
        if (payerName === null) return;
        const payer = people.find(p => p.name.toLowerCase() === payerName.toLowerCase());
        if (!payer) { alert('Payer name was not found.'); return; }
        const participantNames = prompt('Shared by names separated by comma:', current.participants.map(p => p.name).join(', '));
        if (participantNames === null) return;
        const selectedNames = participantNames.split(',').map(x => x.trim().toLowerCase()).filter(Boolean);
        const selected = people.filter(p => selectedNames.includes(p.name.toLowerCase())).map(p => p.id);
        await request(`${API}/expenses/${expenseId}`, {method:'PUT', headers:{'Content-Type':'application/json'}, body:JSON.stringify({description, amount:Number(amount), payerId:payer.id, expenseDate:current.expenseDate, participantIds:selected})});
        await loadExpenses(tripId);
    } catch (e) { alert(e.message); }
}

async function deleteTrip(id) {
    if (!confirm('Delete this trip and all its expenses?')) return;
    try { await request(`${API}/trips/${id}`, { method: 'DELETE' }); await loadTrips(); }
    catch (e) { alert(e.message); }
}

async function submitTrip(event) {
    event.preventDefault();
    try {
        const trip = await request(`${API}/trips`, { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify({
            name: document.getElementById('tripName').value,
            description: document.getElementById('tripDescription').value,
            startDate: document.getElementById('startDate').value,
            endDate: document.getElementById('endDate').value
        })});
        window.location.href = `/trips/${trip.id}`;
    } catch (e) { showMessage('formMessage', e.message); }
}

async function loadTripDetails() {
    const id = tripIdFromUrl();
    try {
        const trip = await request(`${API}/trips/${id}`);
        document.getElementById('tripHeader').innerHTML = `<p class="eyebrow">TRIP</p><h1>${escapeHtml(trip.name)}</h1><p class="muted">${escapeHtml(trip.description || 'No description')}</p><button class="button small secondary" onclick="editTrip(${id})">Edit Trip</button><div class="trip-meta"><span>${formatDate(trip.startDate)}</span><span>→</span><span>${formatDate(trip.endDate)}</span></div>`;
        document.getElementById('addParticipantLink').href = `/trips/${id}/participant`;
        document.getElementById('addExpenseLink').href = `/trips/${id}/expense`;
        document.getElementById('balanceLink').href = `/trips/${id}/balance`;
        document.getElementById('settlementLink').href = `/trips/${id}/settlement`;
        await loadParticipants(id);
        await loadExpenses(id);
    } catch (e) { document.getElementById('tripHeader').innerHTML = `<div class="message">${escapeHtml(e.message)}</div>`; }
}

async function loadParticipants(id) {
    const people = await request(`${API}/trips/${id}/participants`);
    const list = document.getElementById('participantsList');
    list.innerHTML = people.length ? people.map(p => `<div class="person-card"><h3>${escapeHtml(p.name)}</h3><p>${escapeHtml(p.email || 'No email')}</p><div class="trip-actions" style="margin-top:10px"><button class="button small secondary" onclick="editParticipant(${p.id}, ${id})">Edit</button><button class="button small secondary" onclick="deleteParticipant(${p.id}, ${id})">Delete</button></div></div>`).join('') : '<p class="muted">No participants yet.</p>';
}

async function deleteParticipant(participantId, tripId) {
    if (!confirm('Delete this participant? This is only possible if the participant is not used by an expense.')) return;
    try { await request(`${API}/participants/${participantId}`, {method:'DELETE'}); await loadParticipants(tripId); }
    catch (e) { alert(e.message); }
}

async function loadExpenses(id) {
    const expenses = await request(`${API}/trips/${id}/expenses`);
    const tbody = document.getElementById('expensesTable');
    tbody.innerHTML = expenses.length ? expenses.map(e => `<tr><td>${escapeHtml(e.description)}</td><td>${money(e.amount)}</td><td>${escapeHtml(e.payer.name)}</td><td>${formatDate(e.expenseDate)}</td><td><button class="button small secondary" onclick="editExpense(${e.id}, ${id})">Edit</button> <button class="button small secondary" onclick="deleteExpense(${e.id}, ${id})">Delete</button></td></tr>`).join('') : '<tr><td colspan="5" class="muted">No expenses yet.</td></tr>';
}

async function deleteExpense(expenseId, tripId) {
    if (!confirm('Delete this expense?')) return;
    try { await request(`${API}/expenses/${expenseId}`, {method:'DELETE'}); await loadExpenses(tripId); }
    catch (e) { alert(e.message); }
}

async function loadParticipantForm() {
    const id = tripIdFromUrl();
    document.getElementById('backLink').href = `/trips/${id}`;
}

async function submitParticipant(event) {
    event.preventDefault();
    const id = tripIdFromUrl();
    try {
        await request(`${API}/trips/${id}/participants`, {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({
            name: document.getElementById('participantName').value,
            email: document.getElementById('participantEmail').value
        })});
        window.location.href = `/trips/${id}`;
    } catch (e) { showMessage('formMessage', e.message); }
}

async function loadExpenseForm() {
    const id = tripIdFromUrl();
    document.getElementById('backLink').href = `/trips/${id}`;
    document.getElementById('expenseDate').value = new Date().toISOString().slice(0,10);
    try {
        const people = await request(`${API}/trips/${id}/participants`);
        if (!people.length) {
            showMessage('formMessage', 'Add participants before creating an expense.');
            return;
        }
        document.getElementById('payerId').innerHTML = people.map(p => `<option value="${p.id}">${escapeHtml(p.name)}</option>`).join('');
        document.getElementById('participantChecks').innerHTML = people.map((p, index) => `<label class="check-item"><input type="checkbox" name="shared" value="${p.id}" ${index === 0 ? 'checked' : ''}> ${escapeHtml(p.name)}</label>`).join('');
    } catch (e) { showMessage('formMessage', e.message); }
}

async function submitExpense(event) {
    event.preventDefault();
    const id = tripIdFromUrl();
    const selected = [...document.querySelectorAll('input[name="shared"]:checked')].map(x => Number(x.value));
    try {
        await request(`${API}/trips/${id}/expenses`, {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({
            description: document.getElementById('expenseDescription').value,
            amount: Number(document.getElementById('expenseAmount').value),
            payerId: Number(document.getElementById('payerId').value),
            expenseDate: document.getElementById('expenseDate').value,
            participantIds: selected
        })});
        window.location.href = `/trips/${id}`;
    } catch (e) { showMessage('formMessage', e.message); }
}

async function loadBalances() {
    const id = tripIdFromUrl();
    document.getElementById('backLink').href = `/trips/${id}`;
    document.getElementById('settlementLink').href = `/trips/${id}/settlement`;
    try {
        const balances = await request(`${API}/trips/${id}/balances`);
        document.getElementById('balancesTable').innerHTML = balances.map(b => {
            const cls = b.netBalance > 0 ? 'balance-positive' : b.netBalance < 0 ? 'balance-negative' : 'balance-zero';
            const label = b.netBalance > 0 ? 'Receive' : b.netBalance < 0 ? 'Pay' : 'Settled';
            const sign = b.netBalance > 0 ? '+' : '';
            return `<tr><td>${escapeHtml(b.participantName)}</td><td>${money(b.totalPaid)}</td><td>${money(b.totalOwed)}</td><td class="${cls}">${sign}${money(b.netBalance)} <small>${label}</small></td></tr>`;
        }).join('');
    } catch (e) { showMessage('balanceMessage', e.message); }
}

async function loadSettlements() {
    const id = tripIdFromUrl();
    document.getElementById('backLink').href = `/trips/${id}`;
    try {
        const generated = await request(`${API}/trips/${id}/settlements/generate`, {method:'POST'});
        renderSettlements(generated);
    } catch (e) { showMessage('settlementMessage', e.message); }
}

function renderSettlements(items) {
    const list = document.getElementById('settlementsList');
    if (!items.length) { list.innerHTML = '<div class="panel"><p class="muted">No payments are needed. Everyone is already settled.</p></div>'; return; }
    list.innerHTML = items.map(s => `<div class="settlement-item"><div class="names">${escapeHtml(s.fromParticipant)} <span>pays</span> ${escapeHtml(s.toParticipant)}</div><div class="settlement-amount">${money(s.amount)}</div></div>`).join('');
}

function escapeHtml(value) { return String(value ?? '').replace(/[&<>'"]/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[c])); }

document.addEventListener('DOMContentLoaded', () => {
    const page = document.body.dataset.page;
    if (page === 'home' || page === 'trips') loadTrips().catch(e => alert(e.message));
    if (page === 'create-trip') document.getElementById('tripForm').addEventListener('submit', submitTrip);
    if (page === 'trip-details') loadTripDetails();
    if (page === 'add-participant') { loadParticipantForm(); document.getElementById('participantForm').addEventListener('submit', submitParticipant); }
    if (page === 'add-expense') { loadExpenseForm(); document.getElementById('expenseForm').addEventListener('submit', submitExpense); }
    if (page === 'balances') loadBalances();
    if (page === 'settlements') loadSettlements();
});
