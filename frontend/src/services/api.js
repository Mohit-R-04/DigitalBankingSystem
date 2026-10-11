import axios from 'axios';

const api = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Account Service
export const createAccount = (data) => api.post('/accounts', data);
export const getAccount = (accountNumber) => api.get(`/accounts/${accountNumber}`);
export const getBalance = (accountNumber) => api.get(`/accounts/${accountNumber}/balance`);
export const blockAccount = (accountNumber) => api.put(`/accounts/${accountNumber}/block`);
export const creditBalance = (accountNumber, amount) =>
  api.put(`/accounts/${accountNumber}/credit?amount=${amount}`);

// Transaction Service
export const transferMoney = (data) =>
  api.post('/transactions/transfer', data, {
    headers: { 'Idempotency-Key': crypto.randomUUID() },
  });
export const getTransaction = (transactionId) => api.get(`/transactions/${transactionId}`);
export const getTransactionHistory = (accountNumber) =>
  api.get(`/transactions/account/${accountNumber}`);
export const verifyOTP = (transactionId, otp) =>
  api.post(`/transactions/${transactionId}/verify?otp=${otp}`);

// Interbank Service (inbound credit rail simulation). The Interbank
// Service is internal now (outbound transfers arrive via Kafka), so the
// inbound-credit simulator is called directly on :8083, not through the
// API gateway.
export const submitInboundCredit = (data) =>
  api.post('http://localhost:8083/api/v1/interbank/inbound-credit', data);

export default api;