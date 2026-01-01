import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const Dashboard: React.FC = () => {
  const { user, logout, isAdmin } = useAuth();
  const navigate = useNavigate();

  const menuItems = [
    {
      id: 'accounts',
      title: 'View Accounts',
      description: 'COACTVWC - Account View',
      icon: '💳',
      status: 'active',
      path: '/accounts'
    },
    {
      id: 'transactions',
      title: 'Transactions',
      description: 'COTRN00C - Transaction History',
      icon: '📊',
      status: 'active',
      path: '/transactions'
    },
    {
      id: 'cards',
      title: 'Card Management',
      description: 'COCRDLIC - Card List',
      icon: '💎',
      status: 'active',
      path: '/cards'
    },
    {
      id: 'reports',
      title: 'Reports',
      description: 'CORPT00C - Reporting',
      icon: '📈',
      status: 'active',
      path: '/reports'
    },
  ];

  const handleMenuClick = (item: typeof menuItems[0]) => {
    if (item.status === 'active') {
      navigate(item.path);
    }
  };

  return (
    <div className="min-h-screen bg-gray-100">
      {/* Header */}
      <header className="bg-white shadow">
        <div className="max-w-7xl mx-auto px-4 py-4 sm:px-6 lg:px-8 flex justify-between items-center">
          <div className="flex items-center">
            <div className="w-10 h-10 bg-blue-600 rounded-full flex items-center justify-center mr-3">
              <span className="text-white font-bold">CD</span>
            </div>
            <div>
              <h1 className="text-xl font-bold text-gray-800">CardDemo</h1>
              <p className="text-xs text-gray-500">Cloud Native Banking Platform</p>
            </div>
          </div>

          <div className="flex items-center space-x-4">
            <div className="text-right">
              <p className="text-sm font-medium text-gray-700">
                {user?.firstName} {user?.lastName}
              </p>
              <p className="text-xs text-gray-500">
                {user?.userType === 'A' ? 'Administrator' : 'User'} • {user?.userId}
              </p>
            </div>
            <button
              onClick={logout}
              className="bg-gray-100 hover:bg-gray-200 text-gray-700 px-4 py-2 rounded-lg text-sm font-medium transition"
            >
              Logout
            </button>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 py-8 sm:px-6 lg:px-8">
        {/* Welcome Banner */}
        <div className="bg-gradient-to-r from-blue-600 to-blue-700 rounded-2xl p-6 mb-8 text-white">
          <h2 className="text-2xl font-bold mb-2">
            Welcome back, {user?.firstName}!
          </h2>
          <p className="text-blue-100">
            This dashboard replaces the CICS COMEN01C main menu transaction.
          </p>
          <div className="mt-4 flex items-center space-x-4 text-sm">
            <span className={`px-3 py-1 rounded-full ${isAdmin ? 'bg-purple-500' : 'bg-blue-500'}`}>
              {isAdmin ? 'Administrator' : 'Standard User'}
            </span>
            <span className="bg-green-500 px-3 py-1 rounded-full">
              JWT Authenticated
            </span>
            {user?.customerId && (
              <span className="bg-gray-500 px-3 py-1 rounded-full">
                Customer: {user.customerId}
              </span>
            )}
          </div>
        </div>

        {/* Migration Info */}
        <div className="bg-green-50 border border-green-200 rounded-lg p-4 mb-8">
          <div className="flex">
            <div className="flex-shrink-0">
              <svg className="h-5 w-5 text-green-400" viewBox="0 0 20 20" fill="currentColor">
                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
              </svg>
            </div>
            <div className="ml-3">
              <h3 className="text-sm font-medium text-green-800">
                Account and Card Management Now Available
              </h3>
              <p className="mt-1 text-sm text-green-700">
                Authentication (auth-service), Account View (account-service), and Card Management (card-service) are functional.
                Click "View Accounts" or "Card Management" to explore.
              </p>
            </div>
          </div>
        </div>

        {/* Menu Grid */}
        <h3 className="text-lg font-semibold text-gray-800 mb-4">
          Services (CICS Transaction Mapping)
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {menuItems.map((item) => (
            <div
              key={item.id}
              onClick={() => handleMenuClick(item)}
              className={`bg-white rounded-xl shadow-sm border border-gray-200 p-6 hover:shadow-md transition relative overflow-hidden ${
                item.status === 'active'
                  ? 'cursor-pointer hover:border-blue-300 hover:bg-blue-50'
                  : 'cursor-not-allowed opacity-75'
              }`}
            >
              {item.status === 'coming-soon' && (
                <div className="absolute top-2 right-2">
                  <span className="bg-gray-100 text-gray-600 text-xs px-2 py-1 rounded-full">
                    Coming Soon
                  </span>
                </div>
              )}
              {item.status === 'active' && (
                <div className="absolute top-2 right-2">
                  <span className="bg-green-100 text-green-600 text-xs px-2 py-1 rounded-full">
                    Available
                  </span>
                </div>
              )}
              <div className="text-4xl mb-4">{item.icon}</div>
              <h4 className="text-lg font-semibold text-gray-800 mb-1">
                {item.title}
              </h4>
              <p className="text-sm text-gray-500">{item.description}</p>
            </div>
          ))}
        </div>

        {/* Architecture Info */}
        <div className="mt-8 bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <h3 className="text-lg font-semibold text-gray-800 mb-4">
            Architecture Comparison
          </h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="bg-gray-50 rounded-lg p-4">
              <h4 className="font-medium text-gray-700 mb-2">z/OS CICS (Legacy)</h4>
              <ul className="text-sm text-gray-600 space-y-1">
                <li>• Transaction: CC00 (COSGN00C)</li>
                <li>• BMS Maps: COSGN00.bms</li>
                <li>• VSAM File: USRSEC</li>
                <li>• COMMAREA for state</li>
              </ul>
            </div>
            <div className="bg-blue-50 rounded-lg p-4">
              <h4 className="font-medium text-blue-700 mb-2">Cloud Native (Current)</h4>
              <ul className="text-sm text-blue-600 space-y-1">
                <li>• REST API: POST /api/v1/auth/login</li>
                <li>• React + TailwindCSS</li>
                <li>• PostgreSQL: users table</li>
                <li>• JWT + Redis sessions</li>
              </ul>
            </div>
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="bg-white border-t border-gray-200 mt-8">
        <div className="max-w-7xl mx-auto px-4 py-4 sm:px-6 lg:px-8">
          <p className="text-center text-sm text-gray-500">
            CardDemo Cloud Transformation • Migrated from IBM z/OS CICS
          </p>
        </div>
      </footer>
    </div>
  );
};

export default Dashboard;
