import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import cardService, { CardResponse } from '../services/cardService';
import transactionService, { TransactionResponse } from '../services/transactionService';

const CardDetail: React.FC = () => {
  const { cardNumber } = useParams<{ cardNumber: string }>();
  const { user, logout, isAdmin } = useAuth();
  const navigate = useNavigate();

  const [card, setCard] = useState<CardResponse | null>(null);
  const [transactions, setTransactions] = useState<TransactionResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [transactionsLoading, setTransactionsLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionLoading, setActionLoading] = useState(false);

  // Modals
  const [showBlockModal, setShowBlockModal] = useState(false);
  const [showCloseModal, setShowCloseModal] = useState(false);
  const [showReissueModal, setShowReissueModal] = useState(false);
  const [blockReason, setBlockReason] = useState('');
  const [closeReason, setCloseReason] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  useEffect(() => {
    if (cardNumber) {
      loadCardDetails();
      loadCardTransactions();
    }
  }, [cardNumber]);

  const loadCardDetails = async () => {
    if (!cardNumber) return;
    setLoading(true);
    setError('');
    try {
      const response = await cardService.getCardDetails(cardNumber);
      setCard(response);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load card details');
    } finally {
      setLoading(false);
    }
  };

  const loadCardTransactions = async () => {
    if (!cardNumber) return;
    setTransactionsLoading(true);
    try {
      // Try to get transactions by card number
      const response = await transactionService.getTransactionsByCard(cardNumber);
      setTransactions(response);
    } catch (err: any) {
      console.error('Failed to load card transactions:', err);
      setTransactions([]);
    } finally {
      setTransactionsLoading(false);
    }
  };

  const handleBlockCard = async () => {
    if (!card || !blockReason) return;
    setActionLoading(true);
    try {
      await cardService.blockCard(card.lastFourDigits, blockReason);
      setShowBlockModal(false);
      setBlockReason('');
      setSuccessMessage('Card has been blocked successfully');
      loadCardDetails();
      setTimeout(() => setSuccessMessage(''), 3000);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to block card');
    } finally {
      setActionLoading(false);
    }
  };

  const handleActivateCard = async () => {
    if (!card) return;
    setActionLoading(true);
    try {
      await cardService.activateCard(card.lastFourDigits);
      setSuccessMessage('Card has been activated successfully');
      loadCardDetails();
      setTimeout(() => setSuccessMessage(''), 3000);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to activate card');
    } finally {
      setActionLoading(false);
    }
  };

  const handleCloseCard = async () => {
    if (!card || !closeReason) return;
    setActionLoading(true);
    try {
      await cardService.closeCard(card.lastFourDigits, closeReason);
      setShowCloseModal(false);
      setCloseReason('');
      setSuccessMessage('Card has been closed successfully');
      loadCardDetails();
      setTimeout(() => setSuccessMessage(''), 3000);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to close card');
    } finally {
      setActionLoading(false);
    }
  };

  const handleReissueCard = async () => {
    if (!card) return;
    setActionLoading(true);
    try {
      await cardService.reissueCard(card.lastFourDigits);
      setShowReissueModal(false);
      setSuccessMessage('Card reissue has been requested. A new card will be sent.');
      loadCardDetails();
      setTimeout(() => setSuccessMessage(''), 5000);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to reissue card');
    } finally {
      setActionLoading(false);
    }
  };

  const getCardTypeIcon = (type: string) => {
    switch (type) {
      case 'VC': return { icon: 'V', color: 'bg-blue-600', name: 'Visa' };
      case 'MC': return { icon: 'M', color: 'bg-red-600', name: 'Mastercard' };
      case 'AX': return { icon: 'A', color: 'bg-green-600', name: 'American Express' };
      case 'DC': return { icon: 'D', color: 'bg-orange-600', name: 'Discover' };
      default: return { icon: '?', color: 'bg-gray-600', name: 'Unknown' };
    }
  };

  const getStatusInfo = (card: CardResponse) => {
    if (card.isExpired) {
      return { label: 'Expired', color: 'bg-yellow-100 text-yellow-800', bgColor: 'from-yellow-700 to-yellow-900' };
    }
    if (card.status === 'S') {
      return { label: 'Blocked', color: 'bg-red-100 text-red-800', bgColor: 'from-red-700 to-red-900' };
    }
    if (card.status === 'N') {
      return { label: 'Closed', color: 'bg-gray-100 text-gray-800', bgColor: 'from-gray-600 to-gray-800' };
    }
    return { label: 'Active', color: 'bg-green-100 text-green-800', bgColor: 'from-gray-800 to-gray-900' };
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-100 flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (!card) {
    return (
      <div className="min-h-screen bg-gray-100 flex items-center justify-center">
        <div className="text-center">
          <p className="text-gray-500 mb-4">Card not found</p>
          <button
            onClick={() => navigate('/cards')}
            className="bg-blue-600 text-white px-4 py-2 rounded-lg"
          >
            Back to Cards
          </button>
        </div>
      </div>
    );
  }

  const cardType = getCardTypeIcon(card.cardType);
  const statusInfo = getStatusInfo(card);

  return (
    <div className="min-h-screen bg-gray-100">
      {/* Header */}
      <header className="bg-white shadow">
        <div className="max-w-7xl mx-auto px-4 py-4 sm:px-6 lg:px-8 flex justify-between items-center">
          <div className="flex items-center">
            <button
              onClick={() => navigate('/cards')}
              className="mr-4 text-gray-500 hover:text-gray-700"
            >
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
              </svg>
            </button>
            <div>
              <h1 className="text-xl font-bold text-gray-800">Card Details</h1>
              <p className="text-xs text-gray-500">COCRDVWC - Card View Transaction</p>
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
        {/* Success Message */}
        {successMessage && (
          <div className="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg mb-6 flex items-center">
            <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
            </svg>
            {successMessage}
          </div>
        )}

        {/* Error Message */}
        {error && (
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-6">
            {error}
          </div>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Card Visual */}
          <div className="lg:col-span-1">
            <div className={`bg-gradient-to-br ${statusInfo.bgColor} rounded-2xl p-8 text-white shadow-xl relative overflow-hidden`}>
              {/* Card chip */}
              <div className="absolute top-6 left-6 w-12 h-10 bg-yellow-400 rounded-md opacity-80"></div>

              {/* Card type logo */}
              <div className="absolute top-6 right-6">
                <div className={`w-12 h-12 ${cardType.color} rounded-full flex items-center justify-center text-white font-bold text-xl`}>
                  {cardType.icon}
                </div>
              </div>

              {/* Card number */}
              <div className="mt-16 text-2xl tracking-widest font-mono">
                {card.cardNumber}
              </div>

              {/* Cardholder info */}
              <div className="mt-8 flex justify-between">
                <div>
                  <div className="text-xs opacity-60 uppercase">Cardholder Name</div>
                  <div className="text-lg font-medium">{card.embossedName}</div>
                </div>
                <div className="text-right">
                  <div className="text-xs opacity-60 uppercase">Expires</div>
                  <div className="text-lg font-medium">{card.expiryFormatted}</div>
                </div>
              </div>

              {/* Status badge */}
              <div className="mt-6">
                <span className={`${statusInfo.color} px-3 py-1 rounded-full text-sm font-medium`}>
                  {statusInfo.label}
                </span>
              </div>
            </div>

            {/* Card Actions */}
            <div className="mt-6 bg-white rounded-xl shadow-sm p-6">
              <h3 className="text-lg font-semibold text-gray-800 mb-4">Card Actions</h3>
              <p className="text-xs text-gray-500 mb-4">COCRDUPC - Card Update Transaction</p>

              <div className="space-y-3">
                {/* Activate button - show if blocked or closed */}
                {(card.status === 'S' || card.status === 'N') && !card.isExpired && (
                  <button
                    onClick={handleActivateCard}
                    disabled={actionLoading}
                    className="w-full bg-green-600 hover:bg-green-700 text-white py-2 px-4 rounded-lg flex items-center justify-center disabled:opacity-50"
                  >
                    <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                    </svg>
                    Activate Card
                  </button>
                )}

                {/* Block button - show if active */}
                {card.isActive && !card.isExpired && (
                  <button
                    onClick={() => setShowBlockModal(true)}
                    disabled={actionLoading}
                    className="w-full bg-red-600 hover:bg-red-700 text-white py-2 px-4 rounded-lg flex items-center justify-center disabled:opacity-50"
                  >
                    <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636" />
                    </svg>
                    Block Card
                  </button>
                )}

                {/* Close button - show if active or blocked */}
                {(card.isActive || card.status === 'S') && (
                  <button
                    onClick={() => setShowCloseModal(true)}
                    disabled={actionLoading}
                    className="w-full bg-gray-600 hover:bg-gray-700 text-white py-2 px-4 rounded-lg flex items-center justify-center disabled:opacity-50"
                  >
                    <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                    </svg>
                    Close Card
                  </button>
                )}

                {/* Reissue button - show if expired or about to expire */}
                {(card.isExpired || card.status === 'N') && (
                  <button
                    onClick={() => setShowReissueModal(true)}
                    disabled={actionLoading}
                    className="w-full bg-blue-600 hover:bg-blue-700 text-white py-2 px-4 rounded-lg flex items-center justify-center disabled:opacity-50"
                  >
                    <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                    </svg>
                    Request Reissue
                  </button>
                )}
              </div>
            </div>
          </div>

          {/* Card Details & Transactions */}
          <div className="lg:col-span-2 space-y-6">
            {/* Card Information */}
            <div className="bg-white rounded-xl shadow-sm p-6">
              <h3 className="text-lg font-semibold text-gray-800 mb-4">Card Information</h3>

              <div className="grid grid-cols-2 gap-6">
                <div>
                  <label className="text-sm text-gray-500">Card Type</label>
                  <p className="text-gray-800 font-medium">{card.cardTypeName}</p>
                </div>
                <div>
                  <label className="text-sm text-gray-500">Status</label>
                  <p className="text-gray-800 font-medium">{card.statusName}</p>
                </div>
                <div>
                  <label className="text-sm text-gray-500">Account ID</label>
                  <p className="text-gray-800 font-medium">{card.accountId}</p>
                </div>
                <div>
                  <label className="text-sm text-gray-500">Issued Date</label>
                  <p className="text-gray-800 font-medium">{card.issuedDate || 'N/A'}</p>
                </div>
                <div>
                  <label className="text-sm text-gray-500">Expiry Date</label>
                  <p className="text-gray-800 font-medium">{card.expiryDate}</p>
                </div>
                <div>
                  <label className="text-sm text-gray-500">Last 4 Digits</label>
                  <p className="text-gray-800 font-medium">{card.lastFourDigits}</p>
                </div>
              </div>
            </div>

            {/* Transaction History */}
            <div className="bg-white rounded-xl shadow-sm p-6">
              <div className="flex justify-between items-center mb-4">
                <h3 className="text-lg font-semibold text-gray-800">Transaction History</h3>
                <span className="text-sm text-gray-500">{transactions.length} transactions</span>
              </div>

              {transactionsLoading ? (
                <div className="flex justify-center py-8">
                  <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
                </div>
              ) : transactions.length === 0 ? (
                <div className="text-center py-8 text-gray-500">
                  No transactions found for this card
                </div>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full">
                    <thead>
                      <tr className="border-b border-gray-200">
                        <th className="text-left py-3 px-2 text-sm font-medium text-gray-500">Date</th>
                        <th className="text-left py-3 px-2 text-sm font-medium text-gray-500">Description</th>
                        <th className="text-left py-3 px-2 text-sm font-medium text-gray-500">Merchant</th>
                        <th className="text-right py-3 px-2 text-sm font-medium text-gray-500">Amount</th>
                      </tr>
                    </thead>
                    <tbody>
                      {transactions.slice(0, 10).map((txn) => (
                        <tr key={txn.transactionId} className="border-b border-gray-100 hover:bg-gray-50">
                          <td className="py-3 px-2 text-sm text-gray-600">
                            {txn.transactionDate}
                          </td>
                          <td className="py-3 px-2">
                            <div className="text-sm font-medium text-gray-800">{txn.transactionDesc}</div>
                            <div className="text-xs text-gray-500">{txn.transactionTypeName}</div>
                          </td>
                          <td className="py-3 px-2 text-sm text-gray-600">
                            {txn.merchantName}
                          </td>
                          <td className={`py-3 px-2 text-sm font-medium text-right ${txn.isDebit ? 'text-red-600' : 'text-green-600'}`}>
                            {txn.amountFormatted}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                  {transactions.length > 10 && (
                    <div className="text-center mt-4">
                      <button
                        onClick={() => navigate(`/transactions?card=${card.lastFourDigits}`)}
                        className="text-blue-600 hover:text-blue-700 text-sm font-medium"
                      >
                        View all {transactions.length} transactions
                      </button>
                    </div>
                  )}
                </div>
              )}
            </div>

            {/* CICS Mapping Info */}
            <div className="bg-white rounded-xl shadow-sm p-6">
              <h3 className="text-lg font-semibold text-gray-800 mb-4">CICS to REST Mapping</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="bg-gray-50 rounded-lg p-4">
                  <h4 className="font-medium text-gray-700 mb-2">z/OS CICS (Legacy)</h4>
                  <ul className="text-sm text-gray-600 space-y-1">
                    <li>View: COCRDVWC (Option 04)</li>
                    <li>Update: COCRDUPC (Option 05)</li>
                    <li>BMS Map: COCRDVW</li>
                    <li>VSAM File: CARDDAT</li>
                  </ul>
                </div>
                <div className="bg-blue-50 rounded-lg p-4">
                  <h4 className="font-medium text-blue-700 mb-2">Cloud Native (Current)</h4>
                  <ul className="text-sm text-blue-600 space-y-1">
                    <li>GET /api/v1/cards/{'{cardNumber}'}</li>
                    <li>PUT /api/v1/cards/{'{cardNumber}'}/status</li>
                    <li>React: CardDetail.tsx</li>
                    <li>PostgreSQL: credit_cards</li>
                  </ul>
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>

      {/* Block Card Modal */}
      {showBlockModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl p-6 max-w-md w-full mx-4">
            <h3 className="text-lg font-semibold text-gray-800 mb-4">Block Card</h3>
            <p className="text-gray-600 mb-4">
              Are you sure you want to block card ending in <strong>{card.lastFourDigits}</strong>?
              This will immediately prevent any new transactions.
            </p>
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-1">Reason *</label>
              <select
                value={blockReason}
                onChange={(e) => setBlockReason(e.target.value)}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500"
              >
                <option value="">Select reason...</option>
                <option value="Lost">Lost</option>
                <option value="Stolen">Stolen</option>
                <option value="Fraud">Suspected Fraud</option>
                <option value="Damaged">Card Damaged</option>
                <option value="Other">Other</option>
              </select>
            </div>
            <div className="flex justify-end space-x-3">
              <button
                onClick={() => { setShowBlockModal(false); setBlockReason(''); }}
                className="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg"
              >
                Cancel
              </button>
              <button
                onClick={handleBlockCard}
                disabled={!blockReason || actionLoading}
                className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50"
              >
                {actionLoading ? 'Blocking...' : 'Block Card'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Close Card Modal */}
      {showCloseModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl p-6 max-w-md w-full mx-4">
            <h3 className="text-lg font-semibold text-gray-800 mb-4">Close Card</h3>
            <p className="text-gray-600 mb-4">
              Are you sure you want to permanently close card ending in <strong>{card.lastFourDigits}</strong>?
              This action cannot be easily reversed.
            </p>
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-1">Reason *</label>
              <select
                value={closeReason}
                onChange={(e) => setCloseReason(e.target.value)}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-gray-500"
              >
                <option value="">Select reason...</option>
                <option value="CustomerRequest">Customer Request</option>
                <option value="AccountClosed">Account Closed</option>
                <option value="Upgrade">Upgrading to New Card</option>
                <option value="Fraud">Fraud Prevention</option>
                <option value="Other">Other</option>
              </select>
            </div>
            <div className="flex justify-end space-x-3">
              <button
                onClick={() => { setShowCloseModal(false); setCloseReason(''); }}
                className="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg"
              >
                Cancel
              </button>
              <button
                onClick={handleCloseCard}
                disabled={!closeReason || actionLoading}
                className="px-4 py-2 bg-gray-600 text-white rounded-lg hover:bg-gray-700 disabled:opacity-50"
              >
                {actionLoading ? 'Closing...' : 'Close Card'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Reissue Card Modal */}
      {showReissueModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl p-6 max-w-md w-full mx-4">
            <h3 className="text-lg font-semibold text-gray-800 mb-4">Request Card Reissue</h3>
            <p className="text-gray-600 mb-4">
              A new card will be issued with a new number and sent to the registered address.
              The current card ending in <strong>{card.lastFourDigits}</strong> will remain in its current state.
            </p>
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-4">
              <div className="flex items-start">
                <svg className="w-5 h-5 text-blue-500 mr-2 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <div className="text-sm text-blue-700">
                  <p className="font-medium">Processing Time</p>
                  <p>New cards typically arrive within 5-7 business days.</p>
                </div>
              </div>
            </div>
            <div className="flex justify-end space-x-3">
              <button
                onClick={() => setShowReissueModal(false)}
                className="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg"
              >
                Cancel
              </button>
              <button
                onClick={handleReissueCard}
                disabled={actionLoading}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
              >
                {actionLoading ? 'Processing...' : 'Request Reissue'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default CardDetail;
