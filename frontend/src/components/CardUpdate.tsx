import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import cardService, { CardResponse } from '../services/cardService';

const CardUpdate: React.FC = () => {
  const { cardNumber } = useParams<{ cardNumber: string }>();
  const { user, logout, isAdmin, customerId } = useAuth();
  const navigate = useNavigate();

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [card, setCard] = useState<CardResponse | null>(null);
  const [cards, setCards] = useState<CardResponse[]>([]);
  const [selectedCard, setSelectedCard] = useState<string>(cardNumber || '');

  // Form state
  const [newStatus, setNewStatus] = useState('');
  const [blockReason, setBlockReason] = useState('');
  const [showBlockConfirm, setShowBlockConfirm] = useState(false);

  useEffect(() => {
    loadCards();
  }, [customerId, isAdmin]);

  useEffect(() => {
    if (selectedCard) {
      loadCardDetails(selectedCard);
    }
  }, [selectedCard]);

  const loadCards = async () => {
    try {
      let cardsList: CardResponse[];
      if (isAdmin) {
        const response = await cardService.getAllCards(0, 50);
        cardsList = response.content;
      } else if (customerId) {
        cardsList = await cardService.getCardsByCustomer(customerId);
      } else {
        cardsList = [];
      }
      setCards(cardsList);

      if (cardsList.length > 0 && !selectedCard) {
        setSelectedCard(cardsList[0].lastFourDigits);
      }
    } catch (err: any) {
      setError('Failed to load cards');
    } finally {
      setLoading(false);
    }
  };

  const loadCardDetails = async (lastFour: string) => {
    setLoading(true);
    try {
      const data = await cardService.getCardByLastFour(lastFour);
      setCard(data);
      setNewStatus(data.cardStatus);
    } catch (err: any) {
      setError('Failed to load card details');
    } finally {
      setLoading(false);
    }
  };

  const handleStatusUpdate = async () => {
    if (!card || !newStatus) return;

    setSaving(true);
    setError('');
    try {
      await cardService.updateCardStatusByLastFour(card.lastFourDigits, { status: newStatus });
      setSuccess('Card status updated successfully!');
      loadCardDetails(card.lastFourDigits);
      setTimeout(() => setSuccess(''), 3000);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to update card status');
    } finally {
      setSaving(false);
    }
  };

  const handleBlockCard = async () => {
    if (!card) return;

    setSaving(true);
    setError('');
    try {
      await cardService.blockCardByLastFour(card.lastFourDigits, blockReason || 'User requested block');
      setSuccess('Card blocked successfully!');
      setShowBlockConfirm(false);
      setBlockReason('');
      loadCardDetails(card.lastFourDigits);
      setTimeout(() => setSuccess(''), 3000);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to block card');
    } finally {
      setSaving(false);
    }
  };

  const handleReissueCard = async () => {
    if (!card) return;
    if (!confirm('Request a new card? The current card will be deactivated.')) return;

    setSaving(true);
    setError('');
    try {
      await cardService.reissueCardByLastFour(card.lastFourDigits);
      setSuccess('Card reissue requested! A new card will be mailed.');
      loadCardDetails(card.lastFourDigits);
      setTimeout(() => setSuccess(''), 3000);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to request card reissue');
    } finally {
      setSaving(false);
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'Y': return 'text-green-600 bg-green-100';
      case 'N': return 'text-red-600 bg-red-100';
      case 'S': return 'text-yellow-600 bg-yellow-100';
      default: return 'text-gray-600 bg-gray-100';
    }
  };

  const getStatusName = (status: string) => {
    switch (status) {
      case 'Y': return 'Active';
      case 'N': return 'Closed';
      case 'S': return 'Blocked';
      default: return status;
    }
  };

  if (loading && !card) {
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
              <h1 className="text-xl font-bold text-gray-800">Card Update</h1>
              <p className="text-xs text-gray-500">COCRDUPC - Card Update</p>
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

        {/* Card Selection */}
        <div className="bg-white rounded-xl shadow-sm p-6 mb-6">
          <label className="block text-sm font-medium text-gray-700 mb-2">Select Card</label>
          <select
            value={selectedCard}
            onChange={(e) => setSelectedCard(e.target.value)}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
          >
            <option value="">Select a card...</option>
            {cards.map((c) => (
              <option key={c.cardNumber} value={c.lastFourDigits}>
                {c.cardTypeName} - **** {c.lastFourDigits} ({getStatusName(c.cardStatus)})
              </option>
            ))}
          </select>
        </div>

        {card && (
          <>
            {/* Card Visual */}
            <div className="bg-gradient-to-r from-gray-800 to-gray-900 rounded-xl shadow-lg p-6 mb-6 text-white">
              <div className="flex justify-between items-start mb-8">
                <div>
                  <p className="text-sm text-gray-400">{card.cardTypeName}</p>
                  <span className={`inline-block mt-1 px-2 py-0.5 rounded text-xs font-medium ${
                    card.cardStatus === 'Y' ? 'bg-green-500' :
                    card.cardStatus === 'S' ? 'bg-yellow-500' : 'bg-red-500'
                  }`}>
                    {getStatusName(card.cardStatus)}
                  </span>
                </div>
                <div className="text-right">
                  <p className="text-2xl font-bold">{card.cardTypeName?.includes('VISA') ? 'VISA' : 'MC'}</p>
                </div>
              </div>
              <div className="mb-6">
                <p className="text-2xl font-mono tracking-wider">
                  **** **** **** {card.lastFourDigits}
                </p>
              </div>
              <div className="flex justify-between">
                <div>
                  <p className="text-xs text-gray-400">CARDHOLDER</p>
                  <p className="font-medium">{card.embossedName}</p>
                </div>
                <div>
                  <p className="text-xs text-gray-400">EXPIRES</p>
                  <p className="font-medium">{card.expiryMonth}/{card.expiryYear}</p>
                </div>
              </div>
            </div>

            {/* Card Details */}
            <div className="bg-white rounded-xl shadow-sm p-6 mb-6">
              <h2 className="text-lg font-semibold text-gray-800 mb-4">Card Information</h2>
              <div className="grid grid-cols-2 gap-6">
                <div>
                  <label className="block text-sm text-gray-500">Account ID</label>
                  <p className="text-lg font-medium text-gray-800">{card.accountId}</p>
                </div>
                <div>
                  <label className="block text-sm text-gray-500">Card Type</label>
                  <p className="text-lg font-medium text-gray-800">{card.cardTypeName}</p>
                </div>
                <div>
                  <label className="block text-sm text-gray-500">Issue Date</label>
                  <p className="text-lg font-medium text-gray-800">{card.issuedDate || 'N/A'}</p>
                </div>
                <div>
                  <label className="block text-sm text-gray-500">Expiry</label>
                  <p className={`text-lg font-medium ${card.isExpired ? 'text-red-600' : 'text-gray-800'}`}>
                    {card.expiryMonth}/{card.expiryYear}
                    {card.isExpired && <span className="text-red-600 text-sm ml-2">(Expired)</span>}
                  </p>
                </div>
                <div>
                  <label className="block text-sm text-gray-500">CVV</label>
                  <p className="text-lg font-medium text-gray-800">{card.cvvCode || '***'}</p>
                </div>
                <div>
                  <label className="block text-sm text-gray-500">Current Status</label>
                  <span className={`inline-block px-3 py-1 rounded-full text-sm font-medium ${getStatusColor(card.cardStatus)}`}>
                    {getStatusName(card.cardStatus)}
                  </span>
                </div>
              </div>
            </div>

            {/* Update Actions */}
            <div className="bg-white rounded-xl shadow-sm p-6 mb-6">
              <h2 className="text-lg font-semibold text-gray-800 mb-4">Update Card Status</h2>

              <div className="mb-6">
                <label className="block text-sm font-medium text-gray-700 mb-2">New Status</label>
                <div className="grid grid-cols-3 gap-4">
                  <button
                    type="button"
                    onClick={() => setNewStatus('Y')}
                    disabled={card.cardStatus === 'Y'}
                    className={`p-4 rounded-lg border-2 text-center transition ${
                      newStatus === 'Y'
                        ? 'border-green-500 bg-green-50 text-green-700'
                        : 'border-gray-200 hover:border-gray-300'
                    } ${card.cardStatus === 'Y' ? 'opacity-50 cursor-not-allowed' : ''}`}
                  >
                    <div className="text-2xl mb-1">&#10004;</div>
                    <div className="font-medium">Activate</div>
                    <div className="text-xs text-gray-500">Enable card</div>
                  </button>
                  <button
                    type="button"
                    onClick={() => setNewStatus('N')}
                    disabled={card.cardStatus === 'N'}
                    className={`p-4 rounded-lg border-2 text-center transition ${
                      newStatus === 'N'
                        ? 'border-red-500 bg-red-50 text-red-700'
                        : 'border-gray-200 hover:border-gray-300'
                    } ${card.cardStatus === 'N' ? 'opacity-50 cursor-not-allowed' : ''}`}
                  >
                    <div className="text-2xl mb-1">&#10006;</div>
                    <div className="font-medium">Close</div>
                    <div className="text-xs text-gray-500">Close permanently</div>
                  </button>
                  <button
                    type="button"
                    onClick={() => setShowBlockConfirm(true)}
                    disabled={card.cardStatus === 'S'}
                    className={`p-4 rounded-lg border-2 text-center transition border-yellow-200 hover:border-yellow-300 ${
                      card.cardStatus === 'S' ? 'opacity-50 cursor-not-allowed' : ''
                    }`}
                  >
                    <div className="text-2xl mb-1">&#9888;</div>
                    <div className="font-medium">Block</div>
                    <div className="text-xs text-gray-500">Temporary block</div>
                  </button>
                </div>
              </div>

              <div className="flex justify-between items-center pt-4 border-t border-gray-100">
                <button
                  onClick={handleReissueCard}
                  disabled={saving || card.cardStatus === 'N'}
                  className="px-4 py-2 text-blue-600 hover:bg-blue-50 rounded-lg disabled:opacity-50"
                >
                  Request Card Reissue
                </button>
                <div className="flex space-x-4">
                  <button
                    onClick={() => navigate('/cards')}
                    className="px-6 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50"
                  >
                    Cancel
                  </button>
                  <button
                    onClick={handleStatusUpdate}
                    disabled={saving || newStatus === card.cardStatus || newStatus === 'S'}
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
            </div>

            {/* CICS Mapping */}
            <div className="bg-white rounded-xl shadow-sm p-6">
              <h3 className="text-lg font-semibold text-gray-800 mb-4">CICS to REST Mapping</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="bg-gray-50 rounded-lg p-4">
                  <h4 className="font-medium text-gray-700 mb-2">z/OS CICS (Legacy)</h4>
                  <ul className="text-sm text-gray-600 space-y-1">
                    <li>Transaction: COCRDUPC (Option 05)</li>
                    <li>BMS Map: COCRDU00</li>
                    <li>VSAM File: CARDDAT</li>
                    <li>Operation: REWRITE</li>
                  </ul>
                </div>
                <div className="bg-blue-50 rounded-lg p-4">
                  <h4 className="font-medium text-blue-700 mb-2">Cloud Native (Current)</h4>
                  <ul className="text-sm text-blue-600 space-y-1">
                    <li>PUT /api/v1/cards/{'{cardNumber}'}/status</li>
                    <li>React: CardUpdate.tsx</li>
                    <li>PostgreSQL: credit_cards</li>
                    <li>Kafka: CardUpdatedEvent</li>
                  </ul>
                </div>
              </div>
            </div>
          </>
        )}
      </main>

      {/* Block Confirmation Modal */}
      {showBlockConfirm && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl p-6 max-w-md w-full mx-4">
            <h3 className="text-lg font-semibold text-gray-800 mb-4">Block Card</h3>
            <p className="text-gray-600 mb-4">
              Are you sure you want to block card ending in {card?.lastFourDigits}?
            </p>
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-1">Reason (optional)</label>
              <select
                value={blockReason}
                onChange={(e) => setBlockReason(e.target.value)}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg"
              >
                <option value="">Select reason...</option>
                <option value="Lost card">Lost card</option>
                <option value="Stolen card">Stolen card</option>
                <option value="Suspected fraud">Suspected fraud</option>
                <option value="Temporary hold">Temporary hold</option>
                <option value="Other">Other</option>
              </select>
            </div>
            <div className="flex justify-end space-x-3">
              <button
                onClick={() => { setShowBlockConfirm(false); setBlockReason(''); }}
                className="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg"
              >
                Cancel
              </button>
              <button
                onClick={handleBlockCard}
                disabled={saving}
                className="px-4 py-2 bg-yellow-600 text-white rounded-lg hover:bg-yellow-700 disabled:opacity-50"
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

export default CardUpdate;
