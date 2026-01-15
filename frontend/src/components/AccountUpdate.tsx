import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import accountService from '../services/accountService';

interface Account {
  accountId: number;
  customerId: number;
  customerName: string;
  currentBalance: number;
  creditLimit: number;
  availableCredit: number;
  activeStatus: string;
  statusName: string;
  openDate: string;
  openDateFormatted: string;
  expiryDate: string;
  expiryDateFormatted: string;
}

const AccountUpdate: React.FC = () => {
  const { accountId } = useParams<{ accountId: string }>();
  const { user, logout, isAdmin, customerId } = useAuth();
  const navigate = useNavigate();

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [account, setAccount] = useState<Account | null>(null);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [selectedAccountId, setSelectedAccountId] = useState<string>(accountId || '');

  // Form state
  const [newStatus, setNewStatus] = useState('');

  useEffect(() => {
    loadAccounts();
  }, [customerId, isAdmin]);

  useEffect(() => {
    if (selectedAccountId) {
      loadAccountDetails(parseInt(selectedAccountId));
    }
  }, [selectedAccountId]);

  const loadAccounts = async () => {
    try {
      let accountsList: Account[];
      if (isAdmin) {
        const response = await accountService.getAllAccounts(0, 50);
        accountsList = response.content;
      } else if (customerId) {
        accountsList = await accountService.getAccountsByCustomer(customerId);
      } else {
        accountsList = [];
      }
      setAccounts(accountsList);

      if (accountsList.length > 0 && !selectedAccountId) {
        setSelectedAccountId(accountsList[0].accountId.toString());
      }
    } catch (err: any) {
      setError('Failed to load accounts');
    } finally {
      setLoading(false);
    }
  };

  const loadAccountDetails = async (accId: number) => {
    setLoading(true);
    try {
      const data = await accountService.getAccountById(accId);
      setAccount(data);
      setNewStatus(data.activeStatus);
    } catch (err: any) {
      setError('Failed to load account details');
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    if (!account || !newStatus) return;

    setSaving(true);
    setError('');
    try {
      await accountService.updateAccountStatus(account.accountId, newStatus);
      setSuccess('Account updated successfully!');
      loadAccountDetails(account.accountId);
      setTimeout(() => setSuccess(''), 3000);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to update account');
    } finally {
      setSaving(false);
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'Y': return 'text-green-600 bg-green-100';
      case 'N': return 'text-red-600 bg-red-100';
      default: return 'text-gray-600 bg-gray-100';
    }
  };

  const getStatusName = (status: string) => {
    switch (status) {
      case 'Y': return 'Active';
      case 'N': return 'Closed';
      default: return status;
    }
  };

  if (loading && !account) {
    return (
      <div className="min-h-screen bg-gray-100 flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

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
              <h1 className="text-xl font-bold text-gray-800">Account Update</h1>
              <p className="text-xs text-gray-500">COACTUPC - Account Update</p>
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

      <main className="max-w-4xl mx-auto px-4 py-8 sm:px-6 lg:px-8">
        {/* Messages */}
        {success && (
          <div className="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg mb-6 flex items-center">
            <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
            </svg>
            {success}
          </div>
        )}
        {error && (
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-6">{error}</div>
        )}

        {/* Account Selection */}
        <div className="bg-white rounded-xl shadow-sm p-6 mb-6">
          <label className="block text-sm font-medium text-gray-700 mb-2">Select Account</label>
          <select
            value={selectedAccountId}
            onChange={(e) => setSelectedAccountId(e.target.value)}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
          >
            <option value="">Select an account...</option>
            {accounts.map((acc) => (
              <option key={acc.accountId} value={acc.accountId}>
                {acc.accountId} - {acc.customerName} (${acc.currentBalance?.toFixed(2)})
              </option>
            ))}
          </select>
        </div>

        {account && (
          <>
            {/* Account Details */}
            <div className="bg-white rounded-xl shadow-sm p-6 mb-6">
              <h2 className="text-lg font-semibold text-gray-800 mb-4">Account Information</h2>

              <div className="grid grid-cols-2 gap-6">
                <div>
                  <label className="block text-sm text-gray-500">Account ID</label>
                  <p className="text-lg font-medium text-gray-800">{account.accountId}</p>
                </div>
                <div>
                  <label className="block text-sm text-gray-500">Customer Name</label>
                  <p className="text-lg font-medium text-gray-800">{account.customerName}</p>
                </div>
                <div>
                  <label className="block text-sm text-gray-500">Current Balance</label>
                  <p className="text-lg font-medium text-gray-800">${account.currentBalance?.toFixed(2)}</p>
                </div>
                <div>
                  <label className="block text-sm text-gray-500">Credit Limit</label>
                  <p className="text-lg font-medium text-gray-800">${account.creditLimit?.toFixed(2)}</p>
                </div>
                <div>
                  <label className="block text-sm text-gray-500">Available Credit</label>
                  <p className="text-lg font-medium text-green-600">${account.availableCredit?.toFixed(2)}</p>
                </div>
                <div>
                  <label className="block text-sm text-gray-500">Current Status</label>
                  <span className={`inline-block px-3 py-1 rounded-full text-sm font-medium ${getStatusColor(account.activeStatus)}`}>
                    {account.statusName || getStatusName(account.activeStatus)}
                  </span>
                </div>
                <div>
                  <label className="block text-sm text-gray-500">Open Date</label>
                  <p className="text-lg font-medium text-gray-800">{account.openDateFormatted || account.openDate}</p>
                </div>
                <div>
                  <label className="block text-sm text-gray-500">Expiration Date</label>
                  <p className="text-lg font-medium text-gray-800">{account.expiryDateFormatted || account.expiryDate}</p>
                </div>
              </div>
            </div>

            {/* Update Form */}
            <div className="bg-white rounded-xl shadow-sm p-6 mb-6">
              <h2 className="text-lg font-semibold text-gray-800 mb-4">Update Account Status</h2>

              <div className="mb-6">
                <label className="block text-sm font-medium text-gray-700 mb-2">New Status</label>
                <div className="grid grid-cols-2 gap-4">
                  <button
                    type="button"
                    onClick={() => setNewStatus('Y')}
                    className={`p-4 rounded-lg border-2 text-center transition ${
                      newStatus === 'Y'
                        ? 'border-green-500 bg-green-50 text-green-700'
                        : 'border-gray-200 hover:border-gray-300'
                    }`}
                  >
                    <div className="text-2xl mb-1">&#10004;</div>
                    <div className="font-medium">Active</div>
                    <div className="text-xs text-gray-500">Account is open and operational</div>
                  </button>
                  <button
                    type="button"
                    onClick={() => setNewStatus('N')}
                    className={`p-4 rounded-lg border-2 text-center transition ${
                      newStatus === 'N'
                        ? 'border-red-500 bg-red-50 text-red-700'
                        : 'border-gray-200 hover:border-gray-300'
                    }`}
                  >
                    <div className="text-2xl mb-1">&#10006;</div>
                    <div className="font-medium">Closed</div>
                    <div className="text-xs text-gray-500">Account is closed</div>
                  </button>
                </div>
              </div>

              <div className="flex justify-end space-x-4">
                <button
                  onClick={() => navigate('/accounts')}
                  className="px-6 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  onClick={handleSave}
                  disabled={saving || newStatus === account.activeStatus}
                  className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 flex items-center"
                >
                  {saving ? (
                    <>
                      <svg className="animate-spin h-5 w-5 mr-2" fill="none" viewBox="0 0 24 24">
                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                      </svg>
                      Saving...
                    </>
                  ) : (
                    <>
                      <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                      </svg>
                      Save Changes
                    </>
                  )}
                </button>
              </div>
            </div>

            {/* CICS Mapping */}
            <div className="bg-white rounded-xl shadow-sm p-6">
              <h3 className="text-lg font-semibold text-gray-800 mb-4">CICS to REST Mapping</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="bg-gray-50 rounded-lg p-4">
                  <h4 className="font-medium text-gray-700 mb-2">z/OS CICS (Legacy)</h4>
                  <ul className="text-sm text-gray-600 space-y-1">
                    <li>Transaction: COACTUPC (Option 02)</li>
                    <li>BMS Map: COACTU00</li>
                    <li>VSAM File: ACCTDAT</li>
                    <li>Operation: REWRITE</li>
                  </ul>
                </div>
                <div className="bg-blue-50 rounded-lg p-4">
                  <h4 className="font-medium text-blue-700 mb-2">Cloud Native (Current)</h4>
                  <ul className="text-sm text-blue-600 space-y-1">
                    <li>PUT /api/v1/accounts/{'{accountId}'}/status</li>
                    <li>React: AccountUpdate.tsx</li>
                    <li>PostgreSQL: accounts</li>
                    <li>Kafka: AccountUpdatedEvent</li>
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

export default AccountUpdate;
