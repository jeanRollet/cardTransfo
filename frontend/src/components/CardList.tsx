import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import cardService, { CardResponse, CardListResponse } from '../services/cardService';

const CardList: React.FC = () => {
  const { user, logout, isAdmin, customerId } = useAuth();
  const navigate = useNavigate();
  const [cards, setCards] = useState<CardResponse[]>([]);
  const [stats, setStats] = useState<CardListResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCard, setSelectedCard] = useState<CardResponse | null>(null);
  const [showBlockModal, setShowBlockModal] = useState(false);
  const [blockReason, setBlockReason] = useState('');

  // Test account IDs from seed data (for admin filter)
  const testAccountIds = [
    1000000001, 1000000002, 1000000003, 1000000004, 1000000005,
    1000000006, 1000000007, 1000000008, 1000000009, 1000000011, 1000000012
  ];

  useEffect(() => {
    if (isAdmin) {
      loadAllCards();
    } else if (customerId) {
      loadCardsByCustomer(customerId);
    }
  }, [isAdmin, customerId]);

  const loadCardsByCustomer = async (custId: number) => {
    setLoading(true);
    setError('');
    setStats(null);
    try {
      const response = await cardService.getCardsByCustomer(custId);
      console.log('getCardsByCustomer response:', response);
      setCards(response || []);
    } catch (err: any) {
      console.error('getCardsByCustomer error:', err);
      setError(err.response?.data?.message || err.message || 'Failed to load cards');
    } finally {
      setLoading(false);
    }
  };

  const loadAllCards = async () => {
    setLoading(true);
    setError('');
    setStats(null);
    try {
      const response = await cardService.getAllCards(0, 50);
      console.log('getAllCards response:', response);
      setCards(response.content || []);
    } catch (err: any) {
      console.error('getAllCards error:', err);
      setError(err.response?.data?.message || err.message || 'Failed to load cards');
    } finally {
      setLoading(false);
    }
  };

  const loadCardsByAccount = async (accountId: number) => {
    setLoading(true);
    setError('');
    try {
      const response = await cardService.getCardsByAccount(accountId);
      setCards(response.cards);
      setStats(response);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load cards');
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = async () => {
    if (!searchTerm.trim()) {
      loadAllCards();
      return;
    }
    setLoading(true);
    try {
      const results = await cardService.searchCards(searchTerm);
      setCards(results);
      setStats(null);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Search failed');
    } finally {
      setLoading(false);
    }
  };

  const handleBlockCard = async () => {
    if (!selectedCard) return;
    try {
      // Note: In production, blocking would use full card number from secure backend
      // For demo purposes, we use the last 4 digits (the API would need the full number)
      await cardService.updateCardStatus(selectedCard.lastFourDigits, 'S', blockReason);
      setShowBlockModal(false);
      setBlockReason('');
      setSelectedCard(null);
      loadAllCards();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to block card');
    }
  };

  const getCardTypeIcon = (type: string) => {
    switch (type) {
      case 'VC': return '💳';
      case 'MC': return '🔴';
      case 'AX': return '💎';
      case 'DC': return '🔵';
      default: return '💳';
    }
  };

  const getStatusBadge = (card: CardResponse) => {
    if (card.isExpired) {
      return <span className="bg-yellow-100 text-yellow-800 text-xs px-2 py-1 rounded-full">Expired</span>;
    }
    if (card.status === 'S') {
      return <span className="bg-red-100 text-red-800 text-xs px-2 py-1 rounded-full">Blocked</span>;
    }
    if (card.status === 'N') {
      return <span className="bg-gray-100 text-gray-800 text-xs px-2 py-1 rounded-full">Closed</span>;
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
              <h1 className="text-xl font-bold text-gray-800">Card Management</h1>
              <p className="text-xs text-gray-500">COCRDLIC - Card List Transaction</p>
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
        {/* Search and Filter - Admin Only */}
        {isAdmin && (
          <div className="bg-white rounded-xl shadow-sm p-6 mb-6">
            <div className="flex flex-wrap gap-4">
              <div className="flex-1 min-w-[200px]">
                <label className="block text-sm font-medium text-gray-700 mb-1">Search by Name</label>
                <div className="flex gap-2">
                  <input
                    type="text"
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
                    placeholder="Enter cardholder name..."
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
                <label className="block text-sm font-medium text-gray-700 mb-1">Filter by Account</label>
                <select
                  onChange={(e) => e.target.value ? loadCardsByAccount(parseInt(e.target.value)) : loadAllCards()}
                  className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                >
                  <option value="">All Accounts</option>
                  {testAccountIds.map(id => (
                    <option key={id} value={id}>{id}</option>
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
                Showing your cards (Customer ID: {customerId})
              </span>
            </div>
          </div>
        )}

        {/* Stats (shown when filtered by account) */}
        {stats && (
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
            <div className="bg-white rounded-xl shadow-sm p-4">
              <div className="text-2xl font-bold text-gray-800">{stats.totalCards}</div>
              <div className="text-sm text-gray-500">Total Cards</div>
            </div>
            <div className="bg-white rounded-xl shadow-sm p-4">
              <div className="text-2xl font-bold text-green-600">{stats.activeCards}</div>
              <div className="text-sm text-gray-500">Active</div>
            </div>
            <div className="bg-white rounded-xl shadow-sm p-4">
              <div className="text-2xl font-bold text-yellow-600">{stats.expiredCards}</div>
              <div className="text-sm text-gray-500">Expired</div>
            </div>
            <div className="bg-white rounded-xl shadow-sm p-4">
              <div className="text-2xl font-bold text-red-600">{stats.blockedCards}</div>
              <div className="text-sm text-gray-500">Blocked</div>
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
          /* Card Grid */
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {cards.map((card) => (
              <div
                key={card.cardNumber}
                className="bg-gradient-to-br from-gray-800 to-gray-900 rounded-xl p-6 text-white shadow-lg relative overflow-hidden"
              >
                {/* Card chip design */}
                <div className="absolute top-4 right-4 text-3xl opacity-50">
                  {getCardTypeIcon(card.cardType)}
                </div>

                {/* Card type */}
                <div className="text-sm opacity-75 mb-4">{card.cardTypeName}</div>

                {/* Card number */}
                <div className="text-xl tracking-widest mb-4 font-mono">
                  {card.cardNumber}
                </div>

                {/* Cardholder name */}
                <div className="text-sm opacity-75 mb-1">CARDHOLDER</div>
                <div className="text-lg mb-4">{card.embossedName}</div>

                {/* Expiry and Status */}
                <div className="flex justify-between items-end">
                  <div>
                    <div className="text-xs opacity-75">EXPIRES</div>
                    <div className="text-lg">{card.expiryFormatted}</div>
                  </div>
                  <div className="text-right">
                    {getStatusBadge(card)}
                  </div>
                </div>

                {/* Account ID */}
                <div className="mt-4 pt-4 border-t border-gray-600">
                  <div className="text-xs opacity-50">Account: {card.accountId}</div>
                </div>

                {/* Action buttons */}
                <div className="mt-4 flex justify-between items-center">
                  <button
                    onClick={() => navigate(`/cards/${encodeURIComponent(card.lastFourDigits)}`)}
                    className="bg-blue-500 hover:bg-blue-600 text-white text-sm px-3 py-1 rounded"
                  >
                    View Details
                  </button>
                  {card.isActive && !card.isExpired && (
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        setSelectedCard(card);
                        setShowBlockModal(true);
                      }}
                      className="bg-red-500 hover:bg-red-600 text-white text-sm px-3 py-1 rounded"
                    >
                      Block
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Empty state */}
        {!loading && cards.length === 0 && (
          <div className="text-center py-12 text-gray-500">
            No cards found
          </div>
        )}

        {/* Architecture Info */}
        <div className="mt-8 bg-white rounded-xl shadow-sm p-6">
          <h3 className="text-lg font-semibold text-gray-800 mb-4">CICS to REST Mapping</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="bg-gray-50 rounded-lg p-4">
              <h4 className="font-medium text-gray-700 mb-2">z/OS CICS (Legacy)</h4>
              <ul className="text-sm text-gray-600 space-y-1">
                <li>Transaction: COCRDLIC</li>
                <li>BMS Map: COCRDLI</li>
                <li>VSAM File: CARDDAT</li>
                <li>COBOL: COCRDLIC.cbl</li>
              </ul>
            </div>
            <div className="bg-blue-50 rounded-lg p-4">
              <h4 className="font-medium text-blue-700 mb-2">Cloud Native (Current)</h4>
              <ul className="text-sm text-blue-600 space-y-1">
                <li>API: GET /api/v1/cards/account/{'{id}'}</li>
                <li>React Component: CardList.tsx</li>
                <li>PostgreSQL: credit_cards</li>
                <li>Spring Boot: CardController.java</li>
              </ul>
            </div>
          </div>
        </div>
      </main>

      {/* Block Card Modal */}
      {showBlockModal && selectedCard && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl p-6 max-w-md w-full mx-4">
            <h3 className="text-lg font-semibold text-gray-800 mb-4">Block Card</h3>
            <p className="text-gray-600 mb-4">
              Are you sure you want to block card ending in <strong>{selectedCard.lastFourDigits}</strong>?
            </p>
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-1">Reason</label>
              <select
                value={blockReason}
                onChange={(e) => setBlockReason(e.target.value)}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg"
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
                onClick={() => {
                  setShowBlockModal(false);
                  setSelectedCard(null);
                  setBlockReason('');
                }}
                className="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg"
              >
                Cancel
              </button>
              <button
                onClick={handleBlockCard}
                disabled={!blockReason}
                className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50"
              >
                Block Card
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default CardList;
