const statusConfig = {
  ACTIVE: { bg: 'bg-green-100', text: 'text-green-800', label: 'Active' },
  BLOCKED: { bg: 'bg-red-100', text: 'text-red-800', label: 'Blocked' },
  CLOSED: { bg: 'bg-gray-100', text: 'text-gray-800', label: 'Closed' },
  COMPLETED: { bg: 'bg-green-100', text: 'text-green-800', label: 'Completed' },
  PROCESSING: { bg: 'bg-yellow-100', text: 'text-yellow-800', label: 'Processing' },
  PENDING: { bg: 'bg-blue-100', text: 'text-blue-800', label: 'Pending' },
  PENDING_VERIFICATION: { bg: 'bg-orange-100', text: 'text-orange-800', label: 'Pending OTP' },
  FAILED: { bg: 'bg-red-100', text: 'text-red-800', label: 'Failed' },
  FLAGGED: { bg: 'bg-purple-100', text: 'text-purple-800', label: 'Flagged' },
  SAVINGS: { bg: 'bg-blue-100', text: 'text-blue-800', label: 'Savings' },
  CURRENT: { bg: 'bg-indigo-100', text: 'text-indigo-800', label: 'Current' },
  FIXED_DEPOSIT: { bg: 'bg-amber-100', text: 'text-amber-800', label: 'Fixed Deposit' },
  TRANSFER: { bg: 'bg-indigo-100', text: 'text-indigo-800', label: 'Transfer' },
  DEPOSIT: { bg: 'bg-green-100', text: 'text-green-800', label: 'Deposit' },
  WITHDRAWAL: { bg: 'bg-orange-100', text: 'text-orange-800', label: 'Withdrawal' },
  PAYMENT: { bg: 'bg-purple-100', text: 'text-purple-800', label: 'Payment' },
};

export default function StatusBadge({ status }) {
  const config = statusConfig[status] || {
    bg: 'bg-gray-100',
    text: 'text-gray-800',
    label: status,
  };

  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${config.bg} ${config.text}`}
    >
      {config.label}
    </span>
  );
}