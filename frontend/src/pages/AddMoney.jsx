import { useState } from 'react';
import { Wallet, CheckCircle } from 'lucide-react';
import toast from 'react-hot-toast';
import { creditBalance } from '../services/api';

export default function AddMoney() {
  const [form, setForm] = useState({ accountNumber: '', amount: '' });
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      await creditBalance(form.accountNumber.trim(), parseFloat(form.amount));
      setSuccess(true);
      toast.success('Money added successfully!');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to add money');
    } finally { setLoading(false); }
  };

  if (success) {
    return (
      <div className="max-w-md mx-auto">
        <div className="card p-6 sm:p-8 text-center">
          <CheckCircle className="h-14 w-14 sm:h-16 sm:w-16 text-green-500 mx-auto mb-4" />
          <h2 className="text-xl sm:text-2xl font-bold text-gray-900 mb-2">Money Added!</h2>
          <p className="text-gray-600 mb-6 text-sm sm:text-base">₹{Number(form.amount).toLocaleString('en-IN')} credited to <span className="font-mono">{form.accountNumber}</span></p>
          <button onClick={() => { setSuccess(false); setForm({ accountNumber: '', amount: '' }); }} className="btn-primary">Add More</button>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-md mx-auto">
      <div className="card p-6 sm:p-8">
        <div className="flex items-center space-x-3 mb-6">
          <Wallet className="h-7 w-7 sm:h-8 sm:w-8 text-indigo-600" />
          <h2 className="text-xl sm:text-2xl font-bold text-gray-900">Add Money</h2>
        </div>
        <p className="text-gray-500 text-xs sm:text-sm mb-6">Simulate a deposit to any account. In a real system, this would go through Razorpay.</p>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Account Number</label>
            <input type="text" value={form.accountNumber}
              onChange={(e) => setForm({ ...form, accountNumber: e.target.value })}
              required placeholder="e.g. 000012345678" className="input-field font-mono" />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Amount (₹)</label>
            <input type="number" value={form.amount}
              onChange={(e) => setForm({ ...form, amount: e.target.value })}
              required min="1" step="0.01" placeholder="Enter amount" className="input-field" />
          </div>
          <button type="submit" disabled={loading} className="btn-primary">
            <Wallet className="h-4 w-4" /><span>{loading ? 'Processing...' : 'Add Money'}</span>
          </button>
        </form>
      </div>
    </div>
  );
}