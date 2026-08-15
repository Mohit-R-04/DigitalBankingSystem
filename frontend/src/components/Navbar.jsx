import { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Landmark, LayoutDashboard, UserPlus, ArrowLeftRight, History, Wallet, Menu, X } from 'lucide-react';

const navItems = [
  { path: '/', label: 'Dashboard', icon: LayoutDashboard },
  { path: '/create-account', label: 'Create Account', icon: UserPlus },
  { path: '/transfer', label: 'Transfer', icon: ArrowLeftRight },
  { path: '/transactions', label: 'Transactions', icon: History },
  { path: '/add-money', label: 'Add Money', icon: Wallet },
];

export default function Navbar() {
  const location = useLocation();
  const [mobileOpen, setMobileOpen] = useState(false);

  return (
    <nav className="bg-white shadow-sm border-b border-gray-200 sticky top-0 z-40">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16">
          <Link to="/" className="flex items-center space-x-2 shrink-0" onClick={() => setMobileOpen(false)}>
            <Landmark className="h-7 w-7 sm:h-8 sm:w-8 text-indigo-600" />
            <span className="text-lg sm:text-xl font-bold text-gray-900">Digital Bank</span>
          </Link>

          <div className="hidden md:flex items-center space-x-1">
            {navItems.map((item) => {
              const Icon = item.icon;
              const isActive = location.pathname === item.path;
              return (
                <Link key={item.path} to={item.path}
                  className={`flex items-center space-x-1.5 px-3 py-2 rounded-lg text-sm font-medium transition-colors ${isActive ? 'bg-indigo-50 text-indigo-700' : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'}`}>
                  <Icon className="h-4 w-4" /><span>{item.label}</span>
                </Link>
              );
            })}
          </div>

          <button onClick={() => setMobileOpen(!mobileOpen)}
            className="md:hidden flex items-center px-2 text-gray-600 hover:text-gray-900">
            {mobileOpen ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
          </button>
        </div>
      </div>

      {mobileOpen && (
        <div className="md:hidden border-t border-gray-100 bg-white">
          <div className="px-4 py-3 space-y-1">
            {navItems.map((item) => {
              const Icon = item.icon;
              const isActive = location.pathname === item.path;
              return (
                <Link key={item.path} to={item.path} onClick={() => setMobileOpen(false)}
                  className={`flex items-center space-x-3 px-4 py-3 rounded-xl text-sm font-medium transition-colors ${isActive ? 'bg-indigo-50 text-indigo-700' : 'text-gray-600 hover:bg-gray-50'}`}>
                  <Icon className="h-5 w-5" /><span>{item.label}</span>
                </Link>
              );
            })}
          </div>
        </div>
      )}
    </nav>
  );
}