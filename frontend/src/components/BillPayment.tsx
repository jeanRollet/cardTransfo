import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import billPaymentService, { BillPayee, BillPayment, CreatePaymentRequest } from '../services/billPaymentService';
import accountService from '../services/accountService';

interface AccountOption {
  accountId: number;
  displayName: string;
  balance: number;
}

const BillPaymentPage: React.FC = () => {
  const { user, logout, customerId } = useAuth();
  const navigate = useNavigate();

  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [activeTab, setActiveTab] = useState<'pay' | 'history' | 'payees'>('pay');

  // Data state
  const [payees, setPayees] = useState<BillPayee[]>([]);
  const [payments, setPayments] = useState<BillPayment[]>([]);
  const [accounts, setAccounts] = useState<AccountOption[]>([]);

  // Form state
  const [selectedPayee, setSelectedPayee] = useState<number | ''>('');
  const [selectedAccount, setSelectedAccount] = useState<number | ''>('');
  const [amount, setAmount] = useState('');
  const [paymentDate, setPaymentDate] = useState(new Date().toISOString().split('T')[0]);
  const [memo, setMemo] = useState('');
  const [isRecurring, setIsRecurring] = useState(false);
  const [frequency, setFrequency] = useState('MONTHLY');

  // Add payee modal
  const [showAddPayee, setShowAddPayee] = useState(false);
  const [newPayeeName, setNewPayeeName] = useState('');
  const [newPayeeType, setNewPayeeType] = useState('UTILITY');
  const [newPayeeAccount, setNewPayeeAccount] = useState('');
  const [newPayeeNickname, setNewPayeeNickname] = useState('');

  useEffect(() => {
    if (customerId) {
      loadData();
    }
  }, [customerId]);

  const loadData = async () => {
    setLoading(true);
    try {
      const [payeesRes, paymentsRes, accountsRes] = await Promise.all([
        billPaymentService.getPayeesByCustomer(customerId!).catch(() => []),
        billPaymentService.getPaymentsByCustomer(customerId!).catch(() => []),
        accountService.getAccountsByCustomer(customerId!).catch(() => [])
      ]);

      setPayees(payeesRes);
      setPayments(paymentsRes);
      setAccounts(accountsRes.map((acc: any) => ({
        accountId: acc.accountId,
        displayName: `****${String(acc.accountId).slice(-4)} - $${acc.currentBalance?.toFixed(2) || '0.00'}`,
        balance: acc.currentBalance || 0
      })));
    } catch (err: any) {
      setError('Failed to load data');
    } finally {
      setLoading(false);
    }
  };

  const handlePayBill = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedPayee || !selectedAccount || !amount) {
      setError('Please fill in all required fields');
      return;
    }

    setSubmitting(true);
    setError('');
    try {
      const request: CreatePaymentRequest = {
        accountId: selectedAccount as number,
        payeeId: selectedPayee as number,
        amount: parseFloat(amount),
        paymentDate,
        memo: memo || undefined,
        isRecurring,
        recurringFrequency: isRecurring ? frequency : undefined
      };

      await billPaymentService.createPayment(request);
      setSuccess('Payment scheduled successfully!');

      // Reset form
      setAmount('');
      setMemo('');
      setIsRecurring(false);

      // Reload payments
      loadData();

      setTimeout(() => setSuccess(''), 3000);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to create payment');
    } finally {
      setSubmitting(false);
    }
  };

  const handleAddPayee = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newPayeeName) {
      setError('Payee name is required');
      return;
    }

    setSubmitting(true);
    try {
      await billPaymentService.createPayee({
        customerId: customerId!,
        payeeName: newPayeeName,
        payeeType: newPayeeType,
        payeeAccountNumber: newPayeeAccount || undefined,
        nickname: newPayeeNickname || undefined
      });

      setShowAddPayee(false);
      setNewPayeeName('');
      setNewPayeeAccount('');
      setNewPayeeNickname('');
      setSuccess('Payee added successfully!');
      loadData();

      setTimeout(() => setSuccess(''), 3000);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to add payee');
    } finally {
      setSubmitting(false);
    }
  };

  const handleCancelPayment = async (paymentId: number) => {
    if (!confirm('Are you sure you want to cancel this payment?')) return;

    try {
      await billPaymentService.cancelPayment(paymentId);
      setSuccess('Payment cancelled');
      loadData();
      setTimeout(() => setSuccess(''), 3000);
    } catch (err: any) {
      setError('Failed to cancel payment');
    }
  };

  const getStatusBadge = (status: string) => {
    const styles: { [key: string]: string } = {
      PENDING: 'bg-yellow-100 text-yellow-800',
      SCHEDULED: 'bg-blue-100 text-blue-800',
      PROCESSING: 'bg-purple-100 text-purple-800',
      COMPLETED: 'bg-green-100 text-green-800',
      FAILED: 'bg-red-100 text-red-800',
      CANCELLED: 'bg-gray-100 text-gray-800'
    };
    return <span className={`px-2 py-1 rounded-full text-xs font-medium ${styles[status] || styles.PENDING}`}>{status}</span>;
  };

  const payeeTypes = [
    { value: 'UTILITY', label: 'Utility' },
    { value: 'CREDIT_CARD', label: 'Credit Card' },
    { value: 'LOAN', label: 'Loan' },
    { value: 'INSURANCE', label: 'Insurance' },
    { value: 'TELECOM', label: 'Telecom' },
    { value: 'OTHER', label: 'Other' }
  ];

  if (loading) {
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
            <button onClick={() => navigate('/dashboard')} className="mr-4 text-gray-500 hover:text-gray-700">
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
              </svg>
            </button>
            <div>
              <h1 className="text-xl font-bold text-gray-800">Bill Payment</h1>
              <p className="text-xs text-gray-500">COBIL00C - Bill Payment Transaction</p>
            </div>
          </div>
          <div className="flex items-center space-x-4">
            <span className="text-sm text-gray-600">{user?.firstName} {user?.lastName}</span>
            <button onClick={logout} className="bg-gray-100 hover:bg-gray-200 text-gray-700 px-4 py-2 rounded-lg text-sm">
              Logout
            </button>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-4 py-8 sm:px-6 lg:px-8">
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

        {/* Tabs */}
        <div className="bg-white rounded-xl shadow-sm mb-6">
          <div className="border-b border-gray-200">
            <nav className="flex -mb-px">
              {[
                { id: 'pay', label: 'Pay Bill', icon: '💳' },
                { id: 'history', label: 'Payment History', icon: '📋' },
                { id: 'payees', label: 'Manage Payees', icon: '👥' }
              ].map((tab) => (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id as any)}
                  className={`px-6 py-4 text-sm font-medium border-b-2 ${
                    activeTab === tab.id
                      ? 'border-blue-500 text-blue-600'
                      : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                  }`}
                >
                  <span className="mr-2">{tab.icon}</span>
                  {tab.label}
                </button>
              ))}
            </nav>
          </div>

          <div className="p-6">
            {/* Pay Bill Tab */}
            {activeTab === 'pay' && (
              <form onSubmit={handlePayBill} className="max-w-xl space-y-6">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Pay From Account *</label>
                  <select
                    value={selectedAccount}
                    onChange={(e) => setSelectedAccount(e.target.value ? parseInt(e.target.value) : '')}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                    required
                  >
                    <option value="">Select account...</option>
                    {accounts.map((acc) => (
                      <option key={acc.accountId} value={acc.accountId}>{acc.displayName}</option>
                    ))}
                  </select>
                </div>

                <div>
                  <div className="flex justify-between items-center mb-1">
                    <label className="block text-sm font-medium text-gray-700">Pay To *</label>
                    <button
                      type="button"
                      onClick={() => setShowAddPayee(true)}
                      className="text-blue-600 text-sm hover:text-blue-700"
                    >
                      + Add New Payee
                    </button>
                  </div>
                  <select
                    value={selectedPayee}
                    onChange={(e) => setSelectedPayee(e.target.value ? parseInt(e.target.value) : '')}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                    required
                  >
                    <option value="">Select payee...</option>
                    {payees.map((payee) => (
                      <option key={payee.payeeId} value={payee.payeeId}>
                        {payee.nickname || payee.payeeName}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Amount *</label>
                    <div className="relative">
                      <span className="absolute left-4 top-2.5 text-gray-500">$</span>
                      <input
                        type="number"
                        step="0.01"
                        min="0.01"
                        value={amount}
                        onChange={(e) => setAmount(e.target.value)}
                        className="w-full pl-8 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                        required
                      />
                    </div>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Payment Date *</label>
                    <input
                      type="date"
                      value={paymentDate}
                      onChange={(e) => setPaymentDate(e.target.value)}
                      min={new Date().toISOString().split('T')[0]}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                      required
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Memo (optional)</label>
                  <input
                    type="text"
                    value={memo}
                    onChange={(e) => setMemo(e.target.value)}
                    placeholder="e.g., January bill"
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                  />
                </div>

                <div className="flex items-center space-x-4">
                  <label className="flex items-center">
                    <input
                      type="checkbox"
                      checked={isRecurring}
                      onChange={(e) => setIsRecurring(e.target.checked)}
                      className="h-4 w-4 text-blue-600 border-gray-300 rounded"
                    />
                    <span className="ml-2 text-sm text-gray-700">Make recurring</span>
                  </label>
                  {isRecurring && (
                    <select
                      value={frequency}
                      onChange={(e) => setFrequency(e.target.value)}
                      className="px-3 py-1 border border-gray-300 rounded-lg text-sm"
                    >
                      <option value="WEEKLY">Weekly</option>
                      <option value="BIWEEKLY">Bi-weekly</option>
                      <option value="MONTHLY">Monthly</option>
                      <option value="QUARTERLY">Quarterly</option>
                      <option value="YEARLY">Yearly</option>
                    </select>
                  )}
                </div>

                <button
                  type="submit"
                  disabled={submitting}
                  className="w-full bg-blue-600 text-white py-3 rounded-lg hover:bg-blue-700 disabled:opacity-50 font-medium"
                >
                  {submitting ? 'Processing...' : 'Schedule Payment'}
                </button>
              </form>
            )}

            {/* Payment History Tab */}
            {activeTab === 'history' && (
              <div>
                {payments.length === 0 ? (
                  <p className="text-center text-gray-500 py-8">No payment history</p>
                ) : (
                  <div className="overflow-x-auto">
                    <table className="w-full">
                      <thead>
                        <tr className="border-b border-gray-200">
                          <th className="text-left py-3 px-2 text-sm font-medium text-gray-500">Date</th>
                          <th className="text-left py-3 px-2 text-sm font-medium text-gray-500">Payee</th>
                          <th className="text-right py-3 px-2 text-sm font-medium text-gray-500">Amount</th>
                          <th className="text-center py-3 px-2 text-sm font-medium text-gray-500">Status</th>
                          <th className="text-center py-3 px-2 text-sm font-medium text-gray-500">Actions</th>
                        </tr>
                      </thead>
                      <tbody>
                        {payments.map((payment) => (
                          <tr key={payment.paymentId} className="border-b border-gray-100">
                            <td className="py-3 px-2 text-sm">{payment.paymentDate}</td>
                            <td className="py-3 px-2">
                              <div className="text-sm font-medium">{payment.payeeName}</div>
                              {payment.memo && <div className="text-xs text-gray-500">{payment.memo}</div>}
                            </td>
                            <td className="py-3 px-2 text-sm text-right font-medium">{payment.amountFormatted}</td>
                            <td className="py-3 px-2 text-center">{getStatusBadge(payment.status)}</td>
                            <td className="py-3 px-2 text-center">
                              {['PENDING', 'SCHEDULED'].includes(payment.status) && (
                                <button
                                  onClick={() => handleCancelPayment(payment.paymentId)}
                                  className="text-red-600 text-sm hover:text-red-700"
                                >
                                  Cancel
                                </button>
                              )}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            )}

            {/* Manage Payees Tab */}
            {activeTab === 'payees' && (
              <div>
                <div className="flex justify-end mb-4">
                  <button
                    onClick={() => setShowAddPayee(true)}
                    className="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-blue-700"
                  >
                    + Add New Payee
                  </button>
                </div>
                {payees.length === 0 ? (
                  <p className="text-center text-gray-500 py-8">No payees configured</p>
                ) : (
                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                    {payees.map((payee) => (
                      <div key={payee.payeeId} className="bg-gray-50 rounded-lg p-4">
                        <div className="flex justify-between items-start">
                          <div>
                            <h4 className="font-medium text-gray-800">{payee.nickname || payee.payeeName}</h4>
                            {payee.nickname && <p className="text-sm text-gray-500">{payee.payeeName}</p>}
                            <p className="text-xs text-gray-400 mt-1">{payee.payeeType}</p>
                          </div>
                          <span className={`text-xs px-2 py-1 rounded-full ${payee.isActive ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'}`}>
                            {payee.isActive ? 'Active' : 'Inactive'}
                          </span>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>
        </div>

        {/* CICS Mapping */}
        <div className="bg-white rounded-xl shadow-sm p-6">
          <h3 className="text-lg font-semibold text-gray-800 mb-4">CICS to REST Mapping</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="bg-gray-50 rounded-lg p-4">
              <h4 className="font-medium text-gray-700 mb-2">z/OS CICS (Legacy)</h4>
              <ul className="text-sm text-gray-600 space-y-1">
                <li>Transaction: COBIL00C (Option 10)</li>
                <li>BMS Map: COBIL00</li>
                <li>VSAM Files: BILLPAY, PAYEE</li>
              </ul>
            </div>
            <div className="bg-blue-50 rounded-lg p-4">
              <h4 className="font-medium text-blue-700 mb-2">Cloud Native</h4>
              <ul className="text-sm text-blue-600 space-y-1">
                <li>POST /api/v1/bill-payments</li>
                <li>React: BillPayment.tsx</li>
                <li>PostgreSQL: bill_payments, bill_payees</li>
              </ul>
            </div>
          </div>
        </div>
      </main>

      {/* Add Payee Modal */}
      {showAddPayee && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl p-6 max-w-md w-full mx-4">
            <h3 className="text-lg font-semibold text-gray-800 mb-4">Add New Payee</h3>
            <form onSubmit={handleAddPayee} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Payee Name *</label>
                <input
                  type="text"
                  value={newPayeeName}
                  onChange={(e) => setNewPayeeName(e.target.value)}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg"
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Type</label>
                <select
                  value={newPayeeType}
                  onChange={(e) => setNewPayeeType(e.target.value)}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg"
                >
                  {payeeTypes.map((type) => (
                    <option key={type.value} value={type.value}>{type.label}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Account Number</label>
                <input
                  type="text"
                  value={newPayeeAccount}
                  onChange={(e) => setNewPayeeAccount(e.target.value)}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Nickname</label>
                <input
                  type="text"
                  value={newPayeeNickname}
                  onChange={(e) => setNewPayeeNickname(e.target.value)}
                  placeholder="e.g., Electric Bill"
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg"
                />
              </div>
              <div className="flex justify-end space-x-3 pt-4">
                <button
                  type="button"
                  onClick={() => setShowAddPayee(false)}
                  className="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
                >
                  Add Payee
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default BillPaymentPage;
