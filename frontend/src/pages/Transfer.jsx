import { useState } from 'react';
import { ArrowLeftRight, Send, CheckCircle, AlertCircle } from 'lucide-react';
import toast from 'react-hot-toast';
import { transferMoney, verifyOTP } from '../services/api';
import StatusBadge from '../components/StatusBadge';
import OTPModal from '../components/OTPModal';

export default function Transfer() {
  const [form, setForm] = useState({ senderAccountNumber: '', receiverAccountNumber: '', amount: '', description: '' });
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [otpTxn, setOtpTxn] = useState(null);

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await transferMoney({ ...form, amount: parseFloat(form.amount) });
      setResult(res.data);
      toast.success('Transfer initiated!');
      if (res.data.status === 'PENDING_VERIFICATION') setOtpTxn(res.data.id);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Transfer failed');
    } finally { setLoading(false); }
  };

  const handleVerify = async (txnId, otp) => {
    try {
      const res = await verifyOTP(txnId, otp);
      setResult(res.data);
      setOtpTxn(null);
      toast.success('OTP verified!');
    } catch (err) { toast.error(err.response?.data?.message || 'OTP failed'); }
  };

  return (
    <div className="max-w-lg mx-auto space-y-6">
      <div className="card p-6 sm:p-8">
        <div className="flex items-center space-x-3 mb-6">
          <ArrowLeftRight className="h-7 w-7 sm:h-8 sm:w-8 text-indigo-600" />
          <h2 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white">Transfer Money</h2>
        </div>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 dark:text-gray-600 mb-1">Sender Account</label>
              <input type="text" name="senderAccountNumber" value={form.senderAccountNumber}
                onChange={handleChange} required placeholder="e.g. 000012345678" className="input-field font-mono" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 dark:text-gray-600 mb-1">Receiver Account</label>
              <input type="text" name="receiverAccountNumber" value={form.receiverAccountNumber}
                onChange={handleChange} required placeholder="e.g. 000087654321" className="input-field font-mono" />
            </div>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 dark:text-gray-600 mb-1">Amount (₹)</label>
              <input type="number" name="amount" value={form.amount}
                onChange={handleChange} required min="1" step="0.01" placeholder="Enter amount" className="input-field" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 dark:text-gray-600 mb-1">Description</label>
              <input type="text" name="description" value={form.description}
                onChange={handleChange} placeholder="e.g. Rent payment" className="input-field" />
            </div>
          </div>
          <button type="submit" disabled={loading} className="btn-primary">
            <Send className="h-4 w-4" /><span>{loading ? 'Processing...' : 'Send Money'}</span>
          </button>
        </form>
      </div>

      {result && (
        <div className="card p-5 sm:p-6">
          <div className="flex items-center space-x-2 mb-4">
            {result.status === 'COMPLETED' ? <CheckCircle className="h-6 w-6 text-green-500" /> : <AlertCircle className="h-6 w-6 text-orange-500" />}
            <h3 className="text-base sm:text-lg font-semibold">Transaction Result</h3>
          </div>
          <div className="grid grid-cols-2 gap-3 text-xs sm:text-sm">
            <div><span className="text-gray-500 dark:text-gray-400 dark:text-gray-500">ID</span><p className="font-mono font-medium truncate">{result.id}</p></div>
            <div><span className="text-gray-500 dark:text-gray-400 dark:text-gray-500">Status</span><p><StatusBadge status={result.status} /></p></div>
            <div><span className="text-gray-500 dark:text-gray-400 dark:text-gray-500">From</span><p className="font-mono truncate">{result.senderAccountNumber}</p></div>
            <div><span className="text-gray-500 dark:text-gray-400 dark:text-gray-500">To</span><p className="font-mono truncate">{result.receiverAccountNumber}</p></div>
            <div><span className="text-gray-500 dark:text-gray-400 dark:text-gray-500">Amount</span><p className="font-bold text-lg">₹{Number(result.amount).toLocaleString('en-IN')}</p></div>
            <div><span className="text-gray-500 dark:text-gray-400 dark:text-gray-500">Ref</span><p className="font-mono text-xs truncate">{result.referenceNumber}</p></div>
          </div>
          {result.failureReason && <div className="mt-3 bg-red-50 rounded-lg p-3"><p className="text-sm text-red-700">{result.failureReason}</p></div>}
        </div>
      )}

      {otpTxn && <OTPModal transactionId={otpTxn} onVerify={handleVerify} onClose={() => setOtpTxn(null)} />}
    </div>
  );
}