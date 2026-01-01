import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import transactionService, { TransactionResponse } from '../services/transactionService';

const TransactionList: React.FC = () => {
  const { user, logout, isAdmin, customerId } = useAuth();
  const navigate = useNavigate();
  const [transactions, setTransactions] = useState<TransactionResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedTransaction, setSelectedTransaction] = useState<TransactionResponse | null>(null);
  const [showDetailsModal, setShowDetailsModal] = useState(false);

  // Stats
  const [stats, setStats] = useState({
    total: 0,
    credits: 0,
    debits: 0,
    totalCredits: 0,
    totalDebits: 0
  });

  useEffect(() => {
    if (isAdmin) {
      loadAllTransactions();
    } else if (customerId) {
      loadTransactionsByCustomer(customerId);
    }
  }, [isAdmin, customerId]);

  const loadAllTransactions = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await transactionService.getAllTransactions(0, 100);
      setTransactions(response.content || []);
      calculateStats(response.content || []);
    } catch (err: any) {
      console.error('getAllTransactions error:', err);
      setError(err.response?.data?.message || err.message || 'Failed to load transactions');
    } finally {
      setLoading(false);
    }
  };

  const loadTransactionsByCustomer = async (custId: number) => {
    setLoading(true);
    setError('');
    try {
      const response = await transactionService.getTransactionsByCustomer(custId);
      setTransactions(response || []);
      calculateStats(response || []);
    } catch (err: any) {
      console.error('getTransactionsByCustomer error:', err);
      setError(err.response?.data?.message || err.message || 'Failed to load transactions');
    } finally {
      setLoading(false);
    }
  };

  const calculateStats = (txns: TransactionResponse[]) => {
    let credits = 0;
    let debits = 0;
    let totalCredits = 0;
    let totalDebits = 0;

    txns.forEach(t => {
      if (t.isCredit) {
        credits++;
        totalCredits += t.transactionAmount;
      } else if (t.isDebit) {
        debits++;
        totalDebits += Math.abs(t.transactionAmount);
      }
    });

    setStats({
      total: txns.length,
      credits,
      debits,
      totalCredits,
      totalDebits
    });
  };

  const handleSearch = async () => {
    if (!searchTerm.trim()) {
      loadAllTransactions();
      return;
    }
    setLoading(true);
    try {
      const results = await transactionService.searchTransactions(searchTerm);
      setTransactions(results);
      calculateStats(results);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Search failed');
    } finally {
      setLoading(false);
    }
  };

  const getAmountClass = (transaction: TransactionResponse) => {
    if (transaction.isCredit) return 'text-green-600';
    if (transaction.isDebit) return 'text-red-600';
    return 'text-gray-600';
  };

  const getTypeIcon = (type: string) => {
    switch (type) {
      case 'SALE': return '🛒';
      case 'PYMT': return '💰';
      case 'RFND': return '↩️';
      case 'CASH': return '💵';
      case 'FEE': return '📋';
      default: return '💳';
    }
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
              <h1 className="text-xl font-bold text-gray-800">Transaction History</h1>
              <p className="text-xs text-gray-500">COTRN00C - Transaction Browse</p>
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
        {/* Search - Admin Only */}
        {isAdmin && (
          <div className="bg-white rounded-xl shadow-sm p-6 mb-6">
            <div className="flex flex-wrap gap-4">
              <div className="flex-1 min-w-[200px]">
                <label className="block text-sm font-medium text-gray-700 mb-1">Search Transactions</label>
                <div className="flex gap-2">
                  <input
                    type="text"
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
                    placeholder="Search by description or merchant..."
                    className="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                  />
                  <button
                    onClick={handleSearch}
                    className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700"
                  >
                    Search
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* User Info Banner - Regular Users Only */}
        {!isAdmin && (
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
            <div className="flex items-center">
              <svg className="h-5 w-5 text-blue-500 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <span className="text-sm text-blue-700">
                Showing your transactions (Customer ID: {customerId})
              </span>
            </div>
          </div>
        )}

        {/* Stats Cards */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
          <div className="bg-white rounded-xl shadow-sm p-4">
            <div className="text-2xl font-bold text-gray-800">{stats.total}</div>
            <div className="text-sm text-gray-500">Total Transactions</div>
          </div>
          <div className="bg-white rounded-xl shadow-sm p-4">
            <div className="text-2xl font-bold text-green-600">{stats.credits}</div>
            <div className="text-sm text-gray-500">Credits (Payments)</div>
          </div>
          <div className="bg-white rounded-xl shadow-sm p-4">
            <div className="text-2xl font-bold text-red-600">{stats.debits}</div>
            <div className="text-sm text-gray-500">Debits (Purchases)</div>
          </div>
          <div className="bg-white rounded-xl shadow-sm p-4">
            <div className={`text-2xl font-bold ${stats.totalCredits - stats.totalDebits >= 0 ? 'text-green-600' : 'text-red-600'}`}>
              ${Math.abs(stats.totalCredits - stats.totalDebits).toFixed(2)}
            </div>
            <div className="text-sm text-gray-500">Net Amount</div>
          </div>
        </div>

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
          /* Transaction Table */
          <div className="bg-white rounded-xl shadow-sm overflow-hidden">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Date</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Description</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Category</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Card</th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Amount</th>
                  <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {transactions.map((transaction) => (
                  <tr key={transaction.transactionId} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm text-gray-900">{transaction.transactionDate}</div>
                      <div className="text-xs text-gray-500">{transaction.transactionTime}</div>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center">
                        <span className="text-xl mr-2">{getTypeIcon(transaction.transactionType)}</span>
                        <div>
                          <div className="text-sm font-medium text-gray-900">
                            {transaction.merchantName || transaction.transactionDesc || transaction.transactionTypeName}
                          </div>
                          <div className="text-xs text-gray-500">
                            {transaction.merchantLocation || transaction.sourceName}
                          </div>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className="px-2 py-1 text-xs rounded-full bg-gray-100 text-gray-800">
                        {transaction.categoryName}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {transaction.cardNumber}
                    </td>
                    <td className={`px-6 py-4 whitespace-nowrap text-sm font-medium text-right ${getAmountClass(transaction)}`}>
                      {transaction.amountFormatted}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-center">
                      <button
                        onClick={() => {
                          setSelectedTransaction(transaction);
                          setShowDetailsModal(true);
                        }}
                        className="text-blue-600 hover:text-blue-800 text-sm"
                      >
                        Details
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Empty state */}
        {!loading && transactions.length === 0 && (
          <div className="text-center py-12 text-gray-500">
            No transactions found
          </div>
        )}

        {/* Architecture Info */}
        <div className="mt-8 bg-white rounded-xl shadow-sm p-6">
          <h3 className="text-lg font-semibold text-gray-800 mb-4">CICS to REST Mapping</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="bg-gray-50 rounded-lg p-4">
              <h4 className="font-medium text-gray-700 mb-2">z/OS CICS (Legacy)</h4>
              <ul className="text-sm text-gray-600 space-y-1">
                <li>Transaction: COTRN00C</li>
                <li>BMS Map: COTRN00</li>
                <li>VSAM File: TRANSACT</li>
                <li>COBOL: COTRN00C.cbl</li>
              </ul>
            </div>
            <div className="bg-blue-50 rounded-lg p-4">
              <h4 className="font-medium text-blue-700 mb-2">Cloud Native (Current)</h4>
              <ul className="text-sm text-blue-600 space-y-1">
                <li>API: GET /api/v1/transactions</li>
                <li>React Component: TransactionList.tsx</li>
                <li>PostgreSQL: transactions</li>
                <li>Spring Boot: TransactionController.java</li>
              </ul>
            </div>
          </div>
        </div>
      </main>

      {/* Transaction Details Modal */}
      {showDetailsModal && selectedTransaction && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl p-6 max-w-lg w-full mx-4 max-h-[90vh] overflow-y-auto">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-lg font-semibold text-gray-800">Transaction Details</h3>
              <button
                onClick={() => {
                  setShowDetailsModal(false);
                  setSelectedTransaction(null);
                }}
                className="text-gray-500 hover:text-gray-700"
              >
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            <div className="space-y-4">
              <div className="flex justify-between items-center p-4 bg-gray-50 rounded-lg">
                <span className="text-3xl">{getTypeIcon(selectedTransaction.transactionType)}</span>
                <span className={`text-2xl font-bold ${getAmountClass(selectedTransaction)}`}>
                  {selectedTransaction.amountFormatted}
                </span>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-xs text-gray-500">Transaction ID</label>
                  <p className="font-medium">{selectedTransaction.transactionId}</p>
                </div>
                <div>
                  <label className="text-xs text-gray-500">Date & Time</label>
                  <p className="font-medium">{selectedTransaction.transactionDateTime}</p>
                </div>
                <div>
                  <label className="text-xs text-gray-500">Type</label>
                  <p className="font-medium">{selectedTransaction.transactionTypeName}</p>
                </div>
                <div>
                  <label className="text-xs text-gray-500">Category</label>
                  <p className="font-medium">{selectedTransaction.categoryName}</p>
                </div>
                <div>
                  <label className="text-xs text-gray-500">Source</label>
                  <p className="font-medium">{selectedTransaction.sourceName}</p>
                </div>
                <div>
                  <label className="text-xs text-gray-500">Account ID</label>
                  <p className="font-medium">{selectedTransaction.accountId}</p>
                </div>
              </div>

              {selectedTransaction.merchantName && (
                <div className="border-t pt-4">
                  <h4 className="text-sm font-medium text-gray-700 mb-2">Merchant Information</h4>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="text-xs text-gray-500">Merchant Name</label>
                      <p className="font-medium">{selectedTransaction.merchantName}</p>
                    </div>
                    <div>
                      <label className="text-xs text-gray-500">Location</label>
                      <p className="font-medium">{selectedTransaction.merchantLocation || 'N/A'}</p>
                    </div>
                  </div>
                </div>
              )}

              <div className="border-t pt-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="text-xs text-gray-500">Card Used</label>
                    <p className="font-medium">{selectedTransaction.cardNumber}</p>
                  </div>
                  <div>
                    <label className="text-xs text-gray-500">Description</label>
                    <p className="font-medium">{selectedTransaction.transactionDesc || 'N/A'}</p>
                  </div>
                </div>
              </div>
            </div>

            <div className="mt-6 flex justify-end">
              <button
                onClick={() => {
                  setShowDetailsModal(false);
                  setSelectedTransaction(null);
                }}
                className="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default TransactionList;
