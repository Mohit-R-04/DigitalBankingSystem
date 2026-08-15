import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { CreditCard, TrendingUp, AlertTriangle, Ban, ArrowLeft } from 'lucide-react';
import toast from 'react-hot-toast';
import { getAccount, blockAccount } from '../services/api';
import StatusBadge from '../components/StatusBadge';

export default function AccountDetails() {
  const { accountNumber } = useParams();
  const navigate = useNavigate();
  const [account, setAccount] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => { fetchAccount(); }, [accountNumber]);

  const fetchAccount = async () => {
    try {
      const res = await getAccount(accountNumber);
      setAccount(res.data);
    } catch (err) {
      toast.error('Account not found');
      navigate('/');
    } finally { setLoading(false); }
  };

  const handleBlock = async () => {
    if (!confirm('Are you sure you want to block this account?')) return;
    try {
      await blockAccount(accountNumber);
      toast.success('Account blocked');
      fetchAccount();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to block account');
    }
  };

  if (loading) return (
    <div className="flex items-center justify-center py-20">
      <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
    </div>
  );
  if (!account) return null;

  return (
    <div className="max-w-2xl mx-auto space-y-5 sm:space-y-6">
      <button onClick={() => navigate(-1)} className="flex items-center space-x-2 text-gray-600 hover:text-gray-900 transition-colors text-sm sm:text-base">
        <ArrowLeft className="h-4 w-4" /><span>Back</span>
      </button>

      <div className="card p-5 sm:p-8">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between mb-6 gap-2">
          <h2 className="text-xl sm:text-2xl font-bold text-gray-900">{account.accountHolderName}</h2>
          <StatusBadge status={account.status} />
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 sm:gap-4 mb-6">
          <div className="bg-gray-50 rounded-xl p-4">
            <div className="flex items-center space-x-2 text-gray-500 mb-1">
              <CreditCard className="h-4 w-4" /><span className="text-xs sm:text-sm">Account</span>
            </div>
            <p className="font-mono font-bold text-sm sm:text-lg">{account.accountNumber}</p>
          </div>
          <div className="bg-gray-50 rounded-xl p-4">
            <div className="flex items-center space-x-2 text-gray-500 mb-1">
              <TrendingUp className="h-4 w-4" /><span className="text-xs sm:text-sm">Balance</span>
            </div>
            <p className="text-xl sm:text-2xl font-bold text-gray-900">₹{Number(account.balance).toLocaleString('en-IN')}</p>
          </div>
          <div className="bg-gray-50 rounded-xl p-4">
            <div className="flex items-center space-x-2 text-gray-500 mb-1">
              <AlertTriangle className="h-4 w-4" /><span className="text-xs sm:text-sm">Daily Limit</span>
            </div>
            <p className="text-xl sm:text-2xl font-bold text-gray-900">₹{Number(account.dailyTransactionLimit).toLocaleString('en-IN')}</p>
          </div>
        </div>

        <div className="border-t pt-4">
          <div className="grid grid-cols-2 gap-3 sm:gap-4 text-xs sm:text-sm">
            <div><span className="text-gray-500">Type</span><p className="mt-0.5"><StatusBadge status={account.accountType} /></p></div>
            <div><span className="text-gray-500">Email</span><p className="font-medium mt-0.5 truncate">{account.email}</p></div>
            <div><span className="text-gray-500">Phone</span><p className="font-medium mt-0.5">{account.phone}</p></div>
            <div><span className="text-gray-500">Created</span><p className="font-medium mt-0.5">{new Date(account.createdAt).toLocaleString('en-IN')}</p></div>
          </div>
        </div>

        {account.status === 'ACTIVE' && (
          <div className="mt-6">
            <button onClick={handleBlock} className="btn-primary bg-red-600 hover:bg-red-700 w-full sm:w-auto">
              <Ban className="h-4 w-4" /><span>Block Account</span>
            </button>
          </div>
        )}
      </div>
    </div>
  );
}