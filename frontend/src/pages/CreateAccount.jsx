import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { UserPlus, CheckCircle } from 'lucide-react';
import toast from 'react-hot-toast';
import { createAccount } from '../services/api';

export default function CreateAccount() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [created, setCreated] = useState(null);
  const [form, setForm] = useState({ accountHolderName: '', email: '', phone: '', accountType: 'SAVINGS', initialDeposit: '' });

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await createAccount({ ...form, initialDeposit: parseFloat(form.initialDeposit) });
      setCreated(res.data);
      toast.success('Account created successfully!');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to create account');
    } finally { setLoading(false); }
  };

  if (created) {
    return (
      <div className="max-w-lg mx-auto">
        <div className="card p-6 sm:p-8 text-center">
          <CheckCircle className="h-14 w-14 sm:h-16 sm:w-16 text-green-500 mx-auto mb-4" />
          <h2 className="text-xl sm:text-2xl font-bold text-gray-900 mb-2">Account Created!</h2>
          <div className="bg-gray-50 rounded-xl p-4 mb-6">
            <p className="text-sm text-gray-500 mb-1">Account Number</p>
            <p className="text-xl sm:text-2xl font-mono font-bold text-indigo-600">{created.accountNumber}</p>
          </div>
          <div className="grid grid-cols-2 gap-3 text-sm mb-6">
            <div className="bg-gray-50 rounded-lg p-3"><p className="text-gray-500">Holder</p><p className="font-medium">{created.accountHolderName}</p></div>
            <div className="bg-gray-50 rounded-lg p-3"><p className="text-gray-500">Balance</p><p className="font-medium">₹{Number(created.balance).toLocaleString('en-IN')}</p></div>
          </div>
          <div className="flex flex-col sm:flex-row gap-3">
            <button onClick={() => navigate('/')} className="btn-secondary">Dashboard</button>
            <button onClick={() => { setCreated(null); setForm({ accountHolderName: '', email: '', phone: '', accountType: 'SAVINGS', initialDeposit: '' }); }} className="btn-primary">Create Another</button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-lg mx-auto">
      <div className="card p-6 sm:p-8">
        <div className="flex items-center space-x-3 mb-6">
          <UserPlus className="h-7 w-7 sm:h-8 sm:w-8 text-indigo-600" />
          <h2 className="text-xl sm:text-2xl font-bold text-gray-900">Create Account</h2>
        </div>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Account Holder Name</label>
            <input type="text" name="accountHolderName" value={form.accountHolderName}
              onChange={handleChange} required placeholder="Enter full name" className="input-field" />
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
              <input type="email" name="email" value={form.email}
                onChange={handleChange} required placeholder="Enter email" className="input-field" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Phone</label>
              <input type="text" name="phone" value={form.phone}
                onChange={handleChange} required placeholder="Phone number" className="input-field" />
            </div>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Account Type</label>
              <select name="accountType" value={form.accountType} onChange={handleChange} className="input-field">
                <option value="SAVINGS">Savings</option>
                <option value="CURRENT">Current</option>
                <option value="FIXED_DEPOSIT">Fixed Deposit</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Initial Deposit (₹)</label>
              <input type="number" name="initialDeposit" value={form.initialDeposit}
                onChange={handleChange} required min="1" step="0.01" placeholder="Amount" className="input-field" />
            </div>
          </div>
          <button type="submit" disabled={loading} className="btn-primary">
            {loading ? 'Creating...' : 'Create Account'}
          </button>
        </form>
      </div>
    </div>
  );
}