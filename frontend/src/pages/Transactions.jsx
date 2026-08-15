import { useState } from 'react';
import { History, Search, Clock, ArrowUpRight, ArrowDownLeft } from 'lucide-react';
import toast from 'react-hot-toast';
import { getTransactionHistory } from '../services/api';
import StatusBadge from '../components/StatusBadge';

export default function Transactions() {
  const [accountNumber, setAccountNumber] = useState('');
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);

  const handleSearch = async (e) => {
    e.preventDefault();
    if (!accountNumber.trim()) return;
    setLoading(true);
    try {
      const res = await getTransactionHistory(accountNumber.trim());
      setTransactions(res.data);
      setSearched(true);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to fetch transactions');
      setTransactions([]);
    } finally { setLoading(false); }
  };

  const formatDateTime = (dt) => dt ? new Date(dt).toLocaleString('en-IN') : '-';

  return (
    <div className="space-y-5 sm:space-y-6">
      <div className="flex items-center space-x-3">
        <History className="h-7 w-7 sm:h-8 sm:w-8 text-indigo-600" />
        <h2 className="text-xl sm:text-2xl font-bold text-gray-900">Transaction History</h2>
      </div>

      <div className="card p-4 sm:p-6">
        <form onSubmit={handleSearch} className="flex flex-col sm:flex-row gap-3">
          <input type="text" value={accountNumber} onChange={(e) => setAccountNumber(e.target.value)}
            placeholder="Enter account number" required className="input-field flex-1 font-mono" />
          <button type="submit" disabled={loading} className="btn-primary sm:w-auto px-6">
            <Search className="h-4 w-4" /><span>{loading ? 'Loading...' : 'Search'}</span>
          </button>
        </form>
      </div>

      {searched && transactions.length === 0 && (
        <div className="card p-8 sm:p-12 text-center">
          <Clock className="h-10 w-10 sm:h-12 sm:w-12 text-gray-300 mx-auto mb-3" />
          <p className="text-gray-500 text-sm sm:text-base">No transactions found for this account</p>
        </div>
      )}

      {transactions.length > 0 && (
        <div className="card overflow-hidden">
          <div className="px-4 sm:px-6 py-3 sm:py-4 border-b border-gray-200">
            <p className="text-xs sm:text-sm text-gray-500">{transactions.length} transaction(s) found</p>
          </div>
          <div className="divide-y divide-gray-100">
            {transactions.map((txn) => (
              <div key={txn.id} className="px-4 sm:px-6 py-3 sm:py-4 hover:bg-gray-50 transition-colors">
                <div className="flex items-center justify-between gap-3">
                  <div className="flex items-center space-x-3 min-w-0">
                    {txn.senderAccountNumber === accountNumber ? (
                      <div className="w-9 h-9 sm:w-10 sm:h-10 bg-red-100 rounded-full flex items-center justify-center shrink-0">
                        <ArrowUpRight className="h-4 w-4 sm:h-5 sm:w-5 text-red-600" />
                      </div>
                    ) : (
                      <div className="w-9 h-9 sm:w-10 sm:h-10 bg-green-100 rounded-full flex items-center justify-center shrink-0">
                        <ArrowDownLeft className="h-4 w-4 sm:h-5 sm:w-5 text-green-600" />
                      </div>
                    )}
                    <div className="min-w-0">
                      <p className="font-medium text-gray-900 text-sm sm:text-base truncate">
                        {txn.senderAccountNumber === accountNumber ? 'Sent to' : 'Received from'}{' '}
                        <span className="font-mono text-xs sm:text-sm">
                          {txn.senderAccountNumber === accountNumber ? txn.receiverAccountNumber : txn.senderAccountNumber}
                        </span>
                      </p>
                      <p className="text-xs sm:text-sm text-gray-500 truncate">{txn.description || 'No description'}</p>
                      <p className="text-xs text-gray-400 font-mono mt-0.5 hidden sm:block">ID: {txn.id}</p>
                    </div>
                  </div>
                  <div className="text-right shrink-0">
                    <p className={`text-base sm:text-lg font-bold ${txn.senderAccountNumber === accountNumber ? 'text-red-600' : 'text-green-600'}`}>
                      {txn.senderAccountNumber === accountNumber ? '-' : '+'}₹{Number(txn.amount).toLocaleString('en-IN')}
                    </p>
                    <StatusBadge status={txn.status} />
                    <p className="text-xs text-gray-400 mt-1 hidden sm:block">{formatDateTime(txn.createdAt)}</p>
                  </div>
                </div>
                {txn.failureReason && (
                  <div className="mt-2 ml-12 bg-red-50 rounded-lg px-3 py-2">
                    <p className="text-xs text-red-600">{txn.failureReason}</p>
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}