import { useState } from 'react';
import { X, ShieldCheck } from 'lucide-react';

export default function OTPModal({ transactionId, onVerify, onClose }) {
  const [otp, setOtp] = useState('');

  const handleSubmit = (e) => {
    e.preventDefault();
    if (otp.length === 6) {
      onVerify(transactionId, otp);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white dark:bg-gray-800 rounded-2xl p-8 w-full max-w-md mx-4 shadow-xl">
        <div className="flex justify-between items-center mb-6">
          <div className="flex items-center space-x-2">
            <ShieldCheck className="h-6 w-6 text-indigo-600" />
            <h2 className="text-xl font-bold text-gray-900 dark:text-white">
              OTP Verification
            </h2>
          </div>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <p className="text-gray-600 dark:text-gray-300 mb-6">
          Transaction <span className="font-mono text-indigo-600">{transactionId}</span> requires
          OTP verification. Enter the 6-digit code sent to your registered device.
        </p>

        <form onSubmit={handleSubmit}>
          <input
            type="text"
            maxLength={6}
            value={otp}
            onChange={(e) => setOtp(e.target.value.replace(/\D/g, ''))}
            placeholder="Enter 6-digit OTP"
            className="w-full text-center text-2xl tracking-[0.5em] font-mono px-4 py-3 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-xl focus:ring-2 focus:ring-indigo-500 focus:border-transparent mb-4"
          />
          <div className="flex space-x-3">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 px-4 py-3 border border-gray-300 text-gray-700 rounded-xl hover:bg-gray-50 font-medium dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={otp.length !== 6}
              className="flex-1 px-4 py-3 bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 font-medium disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Verify
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}