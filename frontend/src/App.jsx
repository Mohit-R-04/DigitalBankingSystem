import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { ThemeProvider } from './context/ThemeContext';
import Navbar from './components/Navbar';
import Dashboard from './pages/Dashboard';
import CreateAccount from './pages/CreateAccount';
import Transfer from './pages/Transfer';
import Transactions from './pages/Transactions';
import AddMoney from './pages/AddMoney';
import AccountDetails from './pages/AccountDetails';

function App() {
  return (
    <ThemeProvider>
      <Router>
        <Toaster position="top-right" toastOptions={{
          duration: 3000,
          style: { borderRadius: '12px', padding: '12px 16px' },
          className: 'dark:bg-gray-700 dark:text-gray-100',
        }} />
        <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex flex-col transition-colors duration-200">
          <Navbar />
          <main className="flex-1 w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8">
            <Routes>
              <Route path="/" element={<Dashboard />} />
              <Route path="/create-account" element={<CreateAccount />} />
              <Route path="/transfer" element={<Transfer />} />
              <Route path="/transactions" element={<Transactions />} />
              <Route path="/add-money" element={<AddMoney />} />
              <Route path="/account/:accountNumber" element={<AccountDetails />} />
            </Routes>
          </main>
          <footer className="text-center py-4 text-sm text-gray-400 dark:text-gray-500 border-t border-gray-100 dark:border-gray-800">
            Digital Banking System &copy; {new Date().getFullYear()}
          </footer>
        </div>
      </Router>
    </ThemeProvider>
  );
}

export default App;