import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import transactionService, { TransactionSummaryResponse } from '../services/transactionService';
import accountService, { AccountSummaryResponse } from '../services/accountService';

const ReportList: React.FC = () => {
  const { user, logout, isAdmin } = useAuth();
  const navigate = useNavigate();
  const [transactionSummary, setTransactionSummary] = useState<TransactionSummaryResponse | null>(null);
  const [accountSummary, setAccountSummary] = useState<AccountSummaryResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    loadReportData();
  }, []);

  const loadReportData = async () => {
    setLoading(true);
    setError('');
    try {
      const [txnSummary, acctSummary] = await Promise.all([
        transactionService.getOverallSummary(),
        accountService.getAccountSummary()
      ]);
      setTransactionSummary(txnSummary);
      setAccountSummary(acctSummary);
    } catch (err: any) {
      console.error('Error loading report data:', err);
      setError(err.response?.data?.message || err.message || 'Failed to load report data');
    } finally {
      setLoading(false);
    }
  };

  const getCategoryColor = (index: number) => {
    const colors = [
      'bg-blue-500', 'bg-green-500', 'bg-yellow-500', 'bg-red-500',
      'bg-purple-500', 'bg-pink-500', 'bg-indigo-500', 'bg-orange-500'
    ];
    return colors[index % colors.length];
  };

  return (
    <div className="min-h-screen bg-gray-100">
      {/* Header */}
      <header className="bg-white shadow">
        <div className="max-w-7xl mx-auto px-4 py-4 sm:px-6 lg:px-8 flex justify-between items-center">
          <div className="flex items-center">
            <button
              onClick={() => navigate('/dashboard')}
              className="mr-4 text-gray-500 hover:text-gray-700"
            >
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
              </svg>
            </button>
            <div>
              <h1 className="text-xl font-bold text-gray-800">Reports</h1>
              <p className="text-xs text-gray-500">CORPT00C - Reporting Dashboard</p>
            </div>
          </div>
          <div className="flex items-center space-x-4">
            <span className="text-sm text-gray-600">{user?.firstName} {user?.lastName}</span>
            <button
              onClick={logout}
              className="bg-gray-100 hover:bg-gray-200 text-gray-700 px-4 py-2 rounded-lg text-sm"
            >
              Logout
            </button>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-4 py-8 sm:px-6 lg:px-8">
        {/* Admin Only Notice */}
        {!isAdmin && (
          <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 mb-6">
            <div className="flex items-center">
              <svg className="h-5 w-5 text-yellow-500 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
              <span className="text-sm text-yellow-700">
                Full reports are available to administrators only. Showing limited data.
              </span>
            </div>
          </div>
        )}

        {/* Error Message */}
        {error && (
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-6">
            {error}
          </div>
        )}

        {/* Loading */}
        {loading ? (
          <div className="flex justify-center items-center py-12">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
          </div>
        ) : (
          <>
            {/* Transaction Summary Cards */}
            <div className="mb-8">
              <h2 className="text-lg font-semibold text-gray-800 mb-4">Transaction Summary</h2>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                <div className="bg-white rounded-xl shadow-sm p-6">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-sm text-gray-500">Total Transactions</p>
                      <p className="text-3xl font-bold text-gray-800">
                        {transactionSummary?.totalTransactions || 0}
                      </p>
                    </div>
                    <div className="bg-blue-100 p-3 rounded-full">
                      <svg className="w-6 h-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                      </svg>
                    </div>
                  </div>
                </div>

                <div className="bg-white rounded-xl shadow-sm p-6">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-sm text-gray-500">Total Purchases</p>
                      <p className="text-3xl font-bold text-red-600">
                        {transactionSummary?.totalPurchasesFormatted || '$0.00'}
                      </p>
                      <p className="text-xs text-gray-400">{transactionSummary?.purchaseCount || 0} transactions</p>
                    </div>
                    <div className="bg-red-100 p-3 rounded-full">
                      <svg className="w-6 h-6 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z" />
                      </svg>
                    </div>
                  </div>
                </div>

                <div className="bg-white rounded-xl shadow-sm p-6">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-sm text-gray-500">Total Payments</p>
                      <p className="text-3xl font-bold text-green-600">
                        {transactionSummary?.totalPaymentsFormatted || '$0.00'}
                      </p>
                      <p className="text-xs text-gray-400">{transactionSummary?.paymentCount || 0} transactions</p>
                    </div>
                    <div className="bg-green-100 p-3 rounded-full">
                      <svg className="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                      </svg>
                    </div>
                  </div>
                </div>

                <div className="bg-white rounded-xl shadow-sm p-6">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-sm text-gray-500">Average Transaction</p>
                      <p className="text-3xl font-bold text-purple-600">
                        {transactionSummary?.averageTransactionFormatted || '$0.00'}
                      </p>
                    </div>
                    <div className="bg-purple-100 p-3 rounded-full">
                      <svg className="w-6 h-6 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                      </svg>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {/* Account Summary Cards */}
            {accountSummary && (
              <div className="mb-8">
                <h2 className="text-lg font-semibold text-gray-800 mb-4">Account Summary</h2>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                  <div className="bg-white rounded-xl shadow-sm p-6">
                    <p className="text-sm text-gray-500">Total Accounts</p>
                    <p className="text-3xl font-bold text-gray-800">{accountSummary.totalAccounts}</p>
                    <p className="text-xs text-green-600">{accountSummary.activeAccounts} active</p>
                  </div>

                  <div className="bg-white rounded-xl shadow-sm p-6">
                    <p className="text-sm text-gray-500">Total Credit Limit</p>
                    <p className="text-3xl font-bold text-blue-600">${accountSummary.totalCreditLimit?.toLocaleString() || '0'}</p>
                  </div>

                  <div className="bg-white rounded-xl shadow-sm p-6">
                    <p className="text-sm text-gray-500">Total Balance</p>
                    <p className="text-3xl font-bold text-orange-600">${accountSummary.totalBalance?.toLocaleString() || '0'}</p>
                  </div>

                  <div className="bg-white rounded-xl shadow-sm p-6">
                    <p className="text-sm text-gray-500">Avg Utilization</p>
                    <p className="text-3xl font-bold text-purple-600">{accountSummary.averageUtilization?.toFixed(1)}%</p>
                  </div>
                </div>
              </div>
            )}

            {/* Category Breakdown */}
            {transactionSummary?.categoryBreakdown && transactionSummary.categoryBreakdown.length > 0 && (
              <div className="mb-8">
                <h2 className="text-lg font-semibold text-gray-800 mb-4">Spending by Category</h2>
                <div className="bg-white rounded-xl shadow-sm p-6">
                  <div className="space-y-4">
                    {transactionSummary.categoryBreakdown.map((category, index) => (
                      <div key={category.category} className="flex items-center">
                        <div className="w-32 text-sm text-gray-600">{category.categoryName}</div>
                        <div className="flex-1 mx-4">
                          <div className="h-4 bg-gray-200 rounded-full overflow-hidden">
                            <div
                              className={`h-full ${getCategoryColor(index)} rounded-full`}
                              style={{ width: `${category.percentage}%` }}
                            ></div>
                          </div>
                        </div>
                        <div className="w-20 text-right text-sm font-medium text-gray-800">
                          {category.percentage.toFixed(1)}%
                        </div>
                        <div className="w-24 text-right text-sm text-gray-600">
                          {category.totalAmountFormatted}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            )}

            {/* Quick Actions */}
            <div className="mb-8">
              <h2 className="text-lg font-semibold text-gray-800 mb-4">Quick Actions</h2>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <button
                  onClick={() => navigate('/transactions')}
                  className="bg-white rounded-xl shadow-sm p-6 text-left hover:shadow-md transition"
                >
                  <div className="flex items-center">
                    <div className="bg-blue-100 p-3 rounded-full mr-4">
                      <svg className="w-6 h-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                      </svg>
                    </div>
                    <div>
                      <p className="font-medium text-gray-800">View Transactions</p>
                      <p className="text-sm text-gray-500">See all transaction history</p>
                    </div>
                  </div>
                </button>

                <button
                  onClick={() => navigate('/accounts')}
                  className="bg-white rounded-xl shadow-sm p-6 text-left hover:shadow-md transition"
                >
                  <div className="flex items-center">
                    <div className="bg-green-100 p-3 rounded-full mr-4">
                      <svg className="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" />
                      </svg>
                    </div>
                    <div>
                      <p className="font-medium text-gray-800">View Accounts</p>
                      <p className="text-sm text-gray-500">Manage account details</p>
                    </div>
                  </div>
                </button>

                <button
                  onClick={() => navigate('/cards')}
                  className="bg-white rounded-xl shadow-sm p-6 text-left hover:shadow-md transition"
                >
                  <div className="flex items-center">
                    <div className="bg-purple-100 p-3 rounded-full mr-4">
                      <svg className="w-6 h-6 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" />
                      </svg>
                    </div>
                    <div>
                      <p className="font-medium text-gray-800">View Cards</p>
                      <p className="text-sm text-gray-500">Manage credit cards</p>
                    </div>
                  </div>
                </button>
              </div>
            </div>

            {/* Architecture Info */}
            <div className="bg-white rounded-xl shadow-sm p-6">
              <h3 className="text-lg font-semibold text-gray-800 mb-4">CICS to REST Mapping</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="bg-gray-50 rounded-lg p-4">
                  <h4 className="font-medium text-gray-700 mb-2">z/OS CICS (Legacy)</h4>
                  <ul className="text-sm text-gray-600 space-y-1">
                    <li>Transaction: CORPT00C</li>
                    <li>BMS Map: CORPT00</li>
                    <li>VSAM Files: Multiple</li>
                    <li>COBOL: CORPT00C.cbl</li>
                  </ul>
                </div>
                <div className="bg-blue-50 rounded-lg p-4">
                  <h4 className="font-medium text-blue-700 mb-2">Cloud Native (Current)</h4>
                  <ul className="text-sm text-blue-600 space-y-1">
                    <li>API: GET /api/v1/transactions/summary</li>
                    <li>React Component: ReportList.tsx</li>
                    <li>PostgreSQL: transactions, accounts</li>
                    <li>Spring Boot: TransactionController.java</li>
                  </ul>
                </div>
              </div>
            </div>
          </>
        )}
      </main>
    </div>
  );
};

export default ReportList;
