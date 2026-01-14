import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import transactionService from '../services/transactionService';
import cardService, { CardResponse } from '../services/cardService';
import accountService from '../services/accountService';

interface AccountOption {
  accountId: number;
  displayName: string;
}

const TransactionAdd: React.FC = () => {
  const { user, logout, isAdmin, customerId } = useAuth();
  const navigate = useNavigate();

  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Form state
  const [accounts, setAccounts] = useState<AccountOption[]>([]);
  const [cards, setCards] = useState<CardResponse[]>([]);
  const [selectedAccount, setSelectedAccount] = useState<number | ''>('');
  const [selectedCard, setSelectedCard] = useState<string>('');
  const [transactionType, setTransactionType] = useState('SALE');
  const [category, setCategory] = useState('RETAIL');
  const [amount, setAmount] = useState('');
  const [merchantName, setMerchantName] = useState('');
  const [merchantCity, setMerchantCity] = useState('');
  const [description, setDescription] = useState('');

  useEffect(() => {
    loadAccounts();
  }, [customerId, isAdmin]);

  useEffect(() => {
    if (selectedAccount) {
      loadCardsForAccount(selectedAccount);
    }
  }, [selectedAccount]);

  const loadAccounts = async () => {
    setLoading(true);
    try {
      if (isAdmin) {
        const response = await accountService.getAllAccounts(0, 50);
        setAccounts(response.content.map((acc: any) => ({
          accountId: acc.accountId,
          displayName: `${acc.accountId} - ${acc.customerName || 'Account'}`
        })));
      } else if (customerId) {
        const response = await accountService.getAccountsByCustomer(customerId);
        setAccounts(response.map((acc: any) => ({
          accountId: acc.accountId,
          displayName: `${acc.accountId} - Balance: $${acc.currentBalance?.toFixed(2) || '0.00'}`
        })));
      }
    } catch (err: any) {
      setError('Failed to load accounts');
    } finally {
      setLoading(false);
    }
  };

  const loadCardsForAccount = async (accountId: number) => {
    try {
      const response = await cardService.getCardsByAccount(accountId);
      setCards(response.cards.filter(c => c.isActive && !c.isExpired));
    } catch (err: any) {
      console.error('Failed to load cards:', err);
      setCards([]);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedAccount || !amount) {
      setError('Please fill in all required fields');
      return;
    }

    setSubmitting(true);
    setError('');
    try {
      const request = {
        accountId: selectedAccount,
        transactionType,
        transactionCategory: category,
        transactionSource: 'ONLINE',
        transactionDesc: description || `${transactionType} - ${merchantName || 'Online Transaction'}`,
        amount: parseFloat(amount),
        merchantName: merchantName || undefined,
        merchantCity: merchantCity || undefined,
        cardNumber: selectedCard || undefined
      };

      await transactionService.createTransaction(request);
      setSuccess('Transaction created successfully!');

      // Reset form
      setAmount('');
      setMerchantName('');
      setMerchantCity('');
      setDescription('');

      setTimeout(() => {
        setSuccess('');
        navigate('/transactions');
      }, 2000);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to create transaction');
    } finally {
      setSubmitting(false);
    }
  };

  const transactionTypes = [
    { value: 'SALE', label: 'Purchase', icon: '🛒' },
    { value: 'PYMT', label: 'Payment', icon: '💳' },
    { value: 'RFND', label: 'Refund', icon: '↩️' },
    { value: 'CASH', label: 'Cash Advance', icon: '💵' },
    { value: 'FEE', label: 'Fee', icon: '📋' }
  ];

  const categories = [
    { value: 'RETAIL', label: 'Retail' },
    { value: 'GROCERY', label: 'Grocery' },
    { value: 'DINING', label: 'Dining' },
    { value: 'GAS', label: 'Gas & Fuel' },
    { value: 'TRAVEL', label: 'Travel' },
    { value: 'ONLINE', label: 'Online Shopping' },
    { value: 'PAYMENT', label: 'Payment' },
    { value: 'OTHER', label: 'Other' }
  ];

  return (
    <div className="min-h-screen bg-gray-100">
      {/* Header */}
      <header className="bg-white shadow">
        <div className="max-w-7xl mx-auto px-4 py-4 sm:px-6 lg:px-8 flex justify-between items-center">
          <div className="flex items-center">
            <button
              onClick={() => navigate('/transactions')}
              className="mr-4 text-gray-500 hover:text-gray-700"
            >
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
              </svg>
            </button>
            <div>
              <h1 className="text-xl font-bold text-gray-800">Add Transaction</h1>
              <p className="text-xs text-gray-500">COTRN02C - Transaction Add</p>
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

      <main className="max-w-3xl mx-auto px-4 py-8 sm:px-6 lg:px-8">
        {/* Success Message */}
        {success && (
          <div className="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg mb-6 flex items-center">
            <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
            </svg>
            {success}
          </div>
        )}

        {/* Error Message */}
        {error && (
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-6">
            {error}
          </div>
        )}

        {/* Transaction Form */}
        <div className="bg-white rounded-xl shadow-sm p-6">
          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Transaction Type Selection */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-3">Transaction Type *</label>
              <div className="grid grid-cols-5 gap-2">
                {transactionTypes.map((type) => (
                  <button
                    key={type.value}
                    type="button"
                    onClick={() => setTransactionType(type.value)}
                    className={`p-3 rounded-lg border-2 text-center transition ${
                      transactionType === type.value
                        ? 'border-blue-500 bg-blue-50'
                        : 'border-gray-200 hover:border-gray-300'
                    }`}
                  >
                    <div className="text-2xl mb-1">{type.icon}</div>
                    <div className="text-xs font-medium">{type.label}</div>
                  </button>
                ))}
              </div>
            </div>

            {/* Account Selection */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Account *</label>
              <select
                value={selectedAccount}
                onChange={(e) => setSelectedAccount(e.target.value ? parseInt(e.target.value) : '')}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                required
              >
                <option value="">Select an account...</option>
                {accounts.map((acc) => (
                  <option key={acc.accountId} value={acc.accountId}>
                    {acc.displayName}
                  </option>
                ))}
              </select>
            </div>

            {/* Card Selection */}
            {cards.length > 0 && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Card (optional)</label>
                <select
                  value={selectedCard}
                  onChange={(e) => setSelectedCard(e.target.value)}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                >
                  <option value="">No card selected</option>
                  {cards.map((card) => (
                    <option key={card.cardNumber} value={card.lastFourDigits}>
                      {card.cardTypeName} - {card.cardNumber}
                    </option>
                  ))}
                </select>
              </div>
            )}

            {/* Amount */}
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
                  placeholder="0.00"
                  className="w-full pl-8 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                  required
                />
              </div>
            </div>

            {/* Category */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Category</label>
              <select
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
              >
                {categories.map((cat) => (
                  <option key={cat.value} value={cat.value}>{cat.label}</option>
                ))}
              </select>
            </div>

            {/* Merchant Info */}
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Merchant Name</label>
                <input
                  type="text"
                  value={merchantName}
                  onChange={(e) => setMerchantName(e.target.value)}
                  placeholder="e.g., Amazon"
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">City</label>
                <input
                  type="text"
                  value={merchantCity}
                  onChange={(e) => setMerchantCity(e.target.value)}
                  placeholder="e.g., Seattle"
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                />
              </div>
            </div>

            {/* Description */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
              <input
                type="text"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Optional description..."
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
              />
            </div>

            {/* Submit Button */}
            <div className="flex justify-end space-x-4 pt-4">
              <button
                type="button"
                onClick={() => navigate('/transactions')}
                className="px-6 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={submitting || !selectedAccount || !amount}
                className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 flex items-center"
              >
                {submitting ? (
                  <>
                    <svg className="animate-spin h-5 w-5 mr-2" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                    </svg>
                    Processing...
                  </>
                ) : (
                  <>
                    <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                    </svg>
                    Create Transaction
                  </>
                )}
              </button>
            </div>
          </form>
        </div>

        {/* CICS Mapping Info */}
        <div className="mt-6 bg-white rounded-xl shadow-sm p-6">
          <h3 className="text-lg font-semibold text-gray-800 mb-4">CICS to REST Mapping</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="bg-gray-50 rounded-lg p-4">
              <h4 className="font-medium text-gray-700 mb-2">z/OS CICS (Legacy)</h4>
              <ul className="text-sm text-gray-600 space-y-1">
                <li>Transaction: COTRN02C (Option 08)</li>
                <li>BMS Map: COTRN02</li>
                <li>VSAM File: TRANDAT</li>
                <li>Operation: WRITE</li>
              </ul>
            </div>
            <div className="bg-blue-50 rounded-lg p-4">
              <h4 className="font-medium text-blue-700 mb-2">Cloud Native (Current)</h4>
              <ul className="text-sm text-blue-600 space-y-1">
                <li>POST /api/v1/transactions</li>
                <li>React: TransactionAdd.tsx</li>
                <li>PostgreSQL: transactions</li>
                <li>Kafka: TransactionCreatedEvent</li>
              </ul>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
};

export default TransactionAdd;
