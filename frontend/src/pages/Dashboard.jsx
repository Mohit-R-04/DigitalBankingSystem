import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, CreditCard, TrendingUp, AlertTriangle, ArrowRight, UserPlus, ArrowLeftRight, History, Wallet } from 'lucide-react';
import toast from 'react-hot-toast';
import { getAccount } from '../services/api';
import StatusBadge from '../components/StatusBadge';

export default function Dashboard() {
  const [accountNumber, setAccountNumber] = useState('');
  const [account, setAccount] = useState(null);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSearch = async (e) => {
    e.preventDefault();
    if (!accountNumber.trim()) return;
    setLoading(true);
    try {
      const res = await getAccount(accountNumber.trim());
      setAccount(res.data);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Account not found');
      setAccount(null);
    } finally { setLoading(false); }
  };

  const actions = [
    { label: 'Create Account', desc: 'Open a new bank account', path: '/create-account', color: 'from-indigo-500 to-indigo-600', icon: UserPlus },
    { label: 'Transfer Money', desc: 'Send money to anyone', path: '/transfer', color: 'from-green-500 to-green-600', icon: ArrowLeftRight },
    { label: 'Transactions', desc: 'View transaction history', path: '/transactions', color: 'from-amber-500 to-amber-600', icon: History },
    { label: 'Add Money', desc: 'Deposit funds', path: '/add-money', color: 'from-purple-500 to-purple-600', icon: Wallet },
  ];

  return (
    <div className="space-y-6 sm:space-y-8">
      <div>
        <h1 className="text-2xl sm:text-3xl font-bold text-gray-900 dark:text-white">Welcome to Digital Bank</h1>
        <p className="text-gray-500 dark:text-gray-400 dark:text-gray-500 mt-1 text-sm sm:text-base">Manage accounts, transfers, and transactions</p>
      </div>

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4">
        {actions.map((action) => {
          const Icon = action.icon;
          return (
            <button key={action.path} onClick={() => navigate(action.path)}
              className="card p-4 sm:p-5 hover:shadow-md hover:-translate-y-0.5 transition-all duration-200 text-left group">
              <div className={`w-10 h-10 sm:w-12 sm:h-12 bg-gradient-to-br ${action.color} rounded-xl flex items-center justify-center mb-3 group-hover:scale-105 transition-transform`}>
                <Icon className="h-5 w-5 sm:h-6 sm:w-6 text-white" />
              </div>
              <h3 className="font-semibold text-gray-900 dark:text-white text-sm sm:text-base">{action.label}</h3>
              <p className="text-xs sm:text-sm text-gray-500 dark:text-gray-400 dark:text-gray-500 mt-0.5">{action.desc}</p>
            </button>
          );
        })}
      </div>

      <div className="card p-4 sm:p-6">
        <h2 className="text-base sm:text-lg font-semibold text-gray-900 dark:text-white mb-4">Look Up Account</h2>
        <form onSubmit={handleSearch} className="flex flex-col sm:flex-row gap-3">
          <input type="text" value={accountNumber} onChange={(e) => setAccountNumber(e.target.value)}
            placeholder="Enter account number (e.g. 000012345678)" className="input-field flex-1 font-mono" />
          <button type="submit" disabled={loading} className="btn-primary sm:w-auto px-6">
            <Search className="h-4 w-4" /><span>{loading ? 'Searching...' : 'Search'}</span>
          </button>
        </form>
        {account && <AccountCard account={account} />}
      </div>
    </div>
  );
}

function AccountCard({ account }) {
  return (
    <div className="mt-6 border-t pt-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between mb-4 gap-2">
        <h3 className="text-lg font-semibold text-gray-900 dark:text-white">{account.accountHolderName}</h3>
        <StatusBadge status={account.status} />
      </div>
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 sm:gap-4">
        <div className="bg-gray-50 dark:bg-gray-700 rounded-xl p-4">
          <div className="flex items-center space-x-2 text-gray-500 dark:text-gray-400 dark:text-gray-500 mb-1">
            <CreditCard className="h-4 w-4" /><span className="text-xs sm:text-sm">Account Number</span>
          </div>
          <p className="font-mono font-semibold text-gray-900 dark:text-white text-sm sm:text-base">{account.accountNumber}</p>
        </div>
        <div className="bg-gray-50 dark:bg-gray-700 rounded-xl p-4">
          <div className="flex items-center space-x-2 text-gray-500 dark:text-gray-400 dark:text-gray-500 mb-1">
            <TrendingUp className="h-4 w-4" /><span className="text-xs sm:text-sm">Balance</span>
          </div>
          <p className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white">₹{Number(account.balance).toLocaleString('en-IN')}</p>
        </div>
        <div className="bg-gray-50 dark:bg-gray-700 rounded-xl p-4">
          <div className="flex items-center space-x-2 text-gray-500 dark:text-gray-400 dark:text-gray-500 mb-1">
            <AlertTriangle className="h-4 w-4" /><span className="text-xs sm:text-sm">Daily Limit</span>
          </div>
          <p className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white">₹{Number(account.dailyTransactionLimit).toLocaleString('en-IN')}</p>
        </div>
      </div>
      <div className="mt-4 grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs sm:text-sm">
        <div><span className="text-gray-500 dark:text-gray-400 dark:text-gray-500">Type</span><p className="font-medium mt-0.5"><StatusBadge status={account.accountType} /></p></div>
        <div><span className="text-gray-500 dark:text-gray-400 dark:text-gray-500">Email</span><p className="font-medium mt-0.5 truncate">{account.email}</p></div>
        <div><span className="text-gray-500 dark:text-gray-400 dark:text-gray-500">Phone</span><p className="font-medium mt-0.5">{account.phone}</p></div>
        <div><span className="text-gray-500 dark:text-gray-400 dark:text-gray-500">Created</span><p className="font-medium mt-0.5">{new Date(account.createdAt).toLocaleDateString('en-IN')}</p></div>
      </div>
    </div>
  );
}