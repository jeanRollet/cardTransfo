import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import accountService, { AccountResponse, AccountSummaryResponse, CustomerResponse } from '../services/accountService';

const AccountList: React.FC = () => {
  const { user, logout, isAdmin, customerId } = useAuth();
  const navigate = useNavigate();
  const [accounts, setAccounts] = useState<AccountResponse[]>([]);
  const [customers, setCustomers] = useState<CustomerResponse[]>([]);
  const [summary, setSummary] = useState<AccountSummaryResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedAccount, setSelectedAccount] = useState<AccountResponse | null>(null);
  const [showDetailsModal, setShowDetailsModal] = useState(false);

  useEffect(() => {
    if (isAdmin) {
      // Admin: load all accounts
      loadAllAccounts();
      loadSummary();
      loadCustomers();
    } else if (customerId) {
      // Regular user: load only their accounts
      loadAccountsByCustomer(customerId);
    }
  }, [isAdmin, customerId]);

  const loadCustomers = async () => {
    try {
      const response = await accountService.getAllCustomers(0, 50);
      setCustomers(response.content || []);
    } catch (err) {
      console.error('Failed to load customers:', err);
    }
  };

  const loadAllAccounts = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await accountService.getAllAccounts(0, 50);
      console.log('getAllAccounts response:', response);
      setAccounts(response.content || []);
    } catch (err: any) {
      console.error('getAllAccounts error:', err);
      setError(err.response?.data?.message || err.message || 'Failed to load accounts');
    } finally {
      setLoading(false);
    }
  };

  const loadSummary = async () => {
    try {
      const summaryData = await accountService.getAccountSummary();
      setSummary(summaryData);
    } catch (err) {
      console.error('Failed to load summary:', err);
    }
  };

  const loadAccountsByCustomer = async (customerId: number) => {
    setLoading(true);
    setError('');
    try {
      const response = await accountService.getAccountsByCustomer(customerId);
      setAccounts(response);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load accounts');
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = async () => {
    if (!searchTerm.trim()) {
      loadAllAccounts();
      return;
    }
    setLoading(true);
    try {
      const results = await accountService.searchAccounts(searchTerm);
      setAccounts(results);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Search failed');
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(amount);
  };

  const getUtilizationColor = (rate: number) => {
    if (rate >= 90) return 'bg-red-500';
    if (rate >= 70) return 'bg-yellow-500';
    if (rate >= 50) return 'bg-blue-500';
    return 'bg-green-500';
  };

  const getStatusBadge = (account: AccountResponse) => {
    if (!account.isActive) {
      return <span className="bg-gray-100 text-gray-800 text-xs px-2 py-1 rounded-full">Closed</span>;
    }
    if (account.isExpired) {
      return <span className="bg-yellow-100 text-yellow-800 text-xs px-2 py-1 rounded-full">Expired</span>;
    }
    return <span className="bg-green-100 text-green-800 text-xs px-2 py-1 rounded-full">Active</span>;
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
              <h1 className="text-xl font-bold text-gray-800">Account Management</h1>
              <p className="text-xs text-gray-500">COACTVWC - Account View Transaction</p>
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
        {/* Summary Stats */}
        {summary && (
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
            <div className="bg-white rounded-xl shadow-sm p-4">
              <div className="text-2xl font-bold text-gray-800">{summary.totalAccounts}</div>
              <div className="text-sm text-gray-500">Total Accounts</div>
            </div>
            <div className="bg-white rounded-xl shadow-sm p-4">
              <div className="text-2xl font-bold text-green-600">{summary.activeAccounts}</div>
              <div className="text-sm text-gray-500">Active</div>
            </div>
            <div className="bg-white rounded-xl shadow-sm p-4">
              <div className="text-2xl font-bold text-blue-600">{formatCurrency(summary.totalBalance)}</div>
              <div className="text-sm text-gray-500">Total Balance</div>
            </div>
            <div className="bg-white rounded-xl shadow-sm p-4">
              <div className="text-2xl font-bold text-purple-600">{summary.averageUtilization.toFixed(1)}%</div>
              <div className="text-sm text-gray-500">Avg Utilization</div>
            </div>
          </div>
        )}

        {/* Search and Filter - Admin Only */}
        {isAdmin && (
          <div className="bg-white rounded-xl shadow-sm p-6 mb-6">
            <div className="flex flex-wrap gap-4">
              <div className="flex-1 min-w-[200px]">
                <label className="block text-sm font-medium text-gray-700 mb-1">Search by Customer Name</label>
                <div className="flex gap-2">
                  <input
                    type="text"
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
                    placeholder="Enter customer name..."
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
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Filter by Customer</label>
                <select
                  onChange={(e) => e.target.value ? loadAccountsByCustomer(parseInt(e.target.value)) : loadAllAccounts()}
                  className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                >
                  <option value="">All Customers</option>
                  {customers.map(customer => (
                    <option key={customer.customerId} value={customer.customerId}>
                      {customer.fullName} ({customer.customerId})
                    </option>
                  ))}
                </select>
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
                Showing your accounts (Customer ID: {customerId})
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
          /* Account Table */
          <div className="bg-white rounded-xl shadow-sm overflow-hidden">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Account ID
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Customer
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Balance
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Credit Limit
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Utilization
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Status
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {accounts.map((account) => (
                  <tr key={account.accountId} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                      {account.accountId}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm font-medium text-gray-900">{account.customerName || `Customer ${account.customerId}`}</div>
                      <div className="text-xs text-gray-500">ID: {account.customerId}</div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {formatCurrency(account.currentBalance)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {formatCurrency(account.creditLimit)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="flex items-center">
                        <div className="w-16 bg-gray-200 rounded-full h-2 mr-2">
                          <div
                            className={`h-2 rounded-full ${getUtilizationColor(account.utilizationRate)}`}
                            style={{ width: `${Math.min(account.utilizationRate, 100)}%` }}
                          ></div>
                        </div>
                        <span className="text-sm text-gray-600">{account.utilizationRate.toFixed(1)}%</span>
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      {getStatusBadge(account)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                      <button
                        onClick={() => {
                          setSelectedAccount(account);
                          setShowDetailsModal(true);
                        }}
                        className="text-blue-600 hover:text-blue-800"
                      >
                        View Details
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Empty state */}
        {!loading && accounts.length === 0 && (
          <div className="text-center py-12 text-gray-500">
            No accounts found
          </div>
        )}

        {/* Architecture Info */}
        <div className="mt-8 bg-white rounded-xl shadow-sm p-6">
          <h3 className="text-lg font-semibold text-gray-800 mb-4">CICS to REST Mapping</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="bg-gray-50 rounded-lg p-4">
              <h4 className="font-medium text-gray-700 mb-2">z/OS CICS (Legacy)</h4>
              <ul className="text-sm text-gray-600 space-y-1">
                <li>Transaction: COACTVWC</li>
                <li>BMS Map: COACTVW</li>
                <li>VSAM File: ACCTDAT</li>
                <li>COBOL: COACTVWC.cbl</li>
              </ul>
            </div>
            <div className="bg-blue-50 rounded-lg p-4">
              <h4 className="font-medium text-blue-700 mb-2">Cloud Native (Current)</h4>
              <ul className="text-sm text-blue-600 space-y-1">
                <li>API: GET /api/v1/accounts/customer/{'{id}'}</li>
                <li>React Component: AccountList.tsx</li>
                <li>PostgreSQL: accounts, customers</li>
                <li>Spring Boot: AccountController.java</li>
              </ul>
            </div>
          </div>
        </div>
      </main>

      {/* Account Details Modal */}
      {showDetailsModal && selectedAccount && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl p-6 max-w-2xl w-full mx-4 max-h-[90vh] overflow-y-auto">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-lg font-semibold text-gray-800">Account Details</h3>
              <button
                onClick={() => {
                  setShowDetailsModal(false);
                  setSelectedAccount(null);
                }}
                className="text-gray-500 hover:text-gray-700"
              >
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="col-span-2 bg-gradient-to-r from-blue-600 to-blue-700 rounded-lg p-4 text-white">
                <div className="text-sm opacity-75">Account Number</div>
                <div className="text-2xl font-bold">{selectedAccount.accountId}</div>
                <div className="mt-2">{getStatusBadge(selectedAccount)}</div>
              </div>

              <div className="bg-gray-50 rounded-lg p-4">
                <div className="text-sm text-gray-500">Customer</div>
                <div className="text-lg font-medium">{selectedAccount.customerName || `Customer ${selectedAccount.customerId}`}</div>
              </div>

              <div className="bg-gray-50 rounded-lg p-4">
                <div className="text-sm text-gray-500">Group ID</div>
                <div className="text-lg font-medium">{selectedAccount.groupId || 'N/A'}</div>
              </div>

              <div className="bg-gray-50 rounded-lg p-4">
                <div className="text-sm text-gray-500">Current Balance</div>
                <div className="text-lg font-medium text-red-600">{formatCurrency(selectedAccount.currentBalance)}</div>
              </div>

              <div className="bg-gray-50 rounded-lg p-4">
                <div className="text-sm text-gray-500">Credit Limit</div>
                <div className="text-lg font-medium">{formatCurrency(selectedAccount.creditLimit)}</div>
              </div>

              <div className="bg-gray-50 rounded-lg p-4">
                <div className="text-sm text-gray-500">Available Credit</div>
                <div className="text-lg font-medium text-green-600">{formatCurrency(selectedAccount.availableCredit)}</div>
              </div>

              <div className="bg-gray-50 rounded-lg p-4">
                <div className="text-sm text-gray-500">Cash Credit Limit</div>
                <div className="text-lg font-medium">{formatCurrency(selectedAccount.cashCreditLimit)}</div>
              </div>

              <div className="bg-gray-50 rounded-lg p-4">
                <div className="text-sm text-gray-500">Open Date</div>
                <div className="text-lg font-medium">{selectedAccount.openDateFormatted}</div>
              </div>

              <div className="bg-gray-50 rounded-lg p-4">
                <div className="text-sm text-gray-500">Expiry Date</div>
                <div className="text-lg font-medium">{selectedAccount.expiryDateFormatted}</div>
              </div>

              <div className="col-span-2 bg-gray-50 rounded-lg p-4">
                <div className="text-sm text-gray-500 mb-2">Credit Utilization</div>
                <div className="flex items-center">
                  <div className="flex-1 bg-gray-200 rounded-full h-4 mr-4">
                    <div
                      className={`h-4 rounded-full ${getUtilizationColor(selectedAccount.utilizationRate)}`}
                      style={{ width: `${Math.min(selectedAccount.utilizationRate, 100)}%` }}
                    ></div>
                  </div>
                  <span className="text-lg font-medium">{selectedAccount.utilizationRate.toFixed(1)}%</span>
                </div>
              </div>

              <div className="bg-gray-50 rounded-lg p-4">
                <div className="text-sm text-gray-500">Current Cycle Credit</div>
                <div className="text-lg font-medium text-green-600">{formatCurrency(selectedAccount.currCycleCredit)}</div>
              </div>

              <div className="bg-gray-50 rounded-lg p-4">
                <div className="text-sm text-gray-500">Current Cycle Debit</div>
                <div className="text-lg font-medium text-red-600">{formatCurrency(selectedAccount.currCycleDebit)}</div>
              </div>
            </div>

            <div className="mt-6 flex justify-end">
              <button
                onClick={() => {
                  setShowDetailsModal(false);
                  setSelectedAccount(null);
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

export default AccountList;
