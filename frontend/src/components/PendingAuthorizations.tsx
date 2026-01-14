import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import authorizationService, { PendingAuthorization, AuthorizationStats } from '../services/authorizationService';

const PendingAuthorizationsPage: React.FC = () => {
  const { user, logout, isAdmin, customerId } = useAuth();
  const navigate = useNavigate();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [authorizations, setAuthorizations] = useState<PendingAuthorization[]>([]);
  const [stats, setStats] = useState<AuthorizationStats | null>(null);
  const [filter, setFilter] = useState<'all' | 'fraud' | 'high-risk'>('all');
  const [selectedAuth, setSelectedAuth] = useState<PendingAuthorization | null>(null);
  const [showDeclineModal, setShowDeclineModal] = useState(false);
  const [declineReason, setDeclineReason] = useState('');
  const [actionLoading, setActionLoading] = useState(false);

  useEffect(() => {
    loadData();
  }, [customerId, isAdmin, filter]);

  const loadData = async () => {
    setLoading(true);
    setError('');
    try {
      let authData: PendingAuthorization[];

      if (filter === 'fraud') {
        authData = await authorizationService.getFraudAlerts();
      } else if (isAdmin) {
        authData = await authorizationService.getAllPending();
      } else if (customerId) {
        authData = await authorizationService.getPendingByCustomer(customerId);
      } else {
        authData = [];
      }

      // Apply high-risk filter client-side
      if (filter === 'high-risk') {
        authData = authData.filter(a => a.riskScore >= 70);
      }

      setAuthorizations(authData);

      // Load stats
      const statsData = await authorizationService.getStats(isAdmin ? undefined : customerId!).catch(() => null);
      setStats(statsData);
    } catch (err: any) {
      setError('Failed to load authorizations');
    } finally {
      setLoading(false);
    }
  };

  const handleApprove = async (auth: PendingAuthorization) => {
    setActionLoading(true);
    try {
      await authorizationService.approveAuthorization(auth.authId);
      setSuccess('Authorization approved');
      loadData();
      setTimeout(() => setSuccess(''), 3000);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to approve');
    } finally {
      setActionLoading(false);
    }
  };

  const handleDecline = async () => {
    if (!selectedAuth || !declineReason) return;
    setActionLoading(true);
    try {
      await authorizationService.declineAuthorization(selectedAuth.authId, declineReason);
      setShowDeclineModal(false);
      setDeclineReason('');
      setSelectedAuth(null);
      setSuccess('Authorization declined');
      loadData();
      setTimeout(() => setSuccess(''), 3000);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to decline');
    } finally {
      setActionLoading(false);
    }
  };

  const handleReportFraud = async (auth: PendingAuthorization) => {
    if (!confirm('Report this transaction as fraudulent? This will block the card.')) return;
    setActionLoading(true);
    try {
      await authorizationService.reportFraud(auth.authId);
      setSuccess('Fraud reported - Card has been blocked');
      loadData();
      setTimeout(() => setSuccess(''), 3000);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to report fraud');
    } finally {
      setActionLoading(false);
    }
  };

  const getRiskBadge = (score: number) => {
    if (score >= 80) return <span className="bg-red-100 text-red-800 px-2 py-1 rounded-full text-xs font-medium">High Risk ({score})</span>;
    if (score >= 50) return <span className="bg-yellow-100 text-yellow-800 px-2 py-1 rounded-full text-xs font-medium">Medium ({score})</span>;
    return <span className="bg-green-100 text-green-800 px-2 py-1 rounded-full text-xs font-medium">Low ({score})</span>;
  };

  const getStatusBadge = (status: string) => {
    const styles: { [key: string]: string } = {
      PENDING: 'bg-yellow-100 text-yellow-800',
      APPROVED: 'bg-green-100 text-green-800',
      DECLINED: 'bg-red-100 text-red-800',
      EXPIRED: 'bg-gray-100 text-gray-800',
      SETTLED: 'bg-blue-100 text-blue-800',
      REVERSED: 'bg-purple-100 text-purple-800'
    };
    return <span className={`px-2 py-1 rounded-full text-xs font-medium ${styles[status] || styles.PENDING}`}>{status}</span>;
  };

  const formatDateTime = (timestamp: string) => {
    if (!timestamp) return 'N/A';
    const date = new Date(timestamp);
    return date.toLocaleString();
  };

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
              <h1 className="text-xl font-bold text-gray-800">Pending Authorizations</h1>
              <p className="text-xs text-gray-500">COAUTH0C - Authorization View</p>
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

        {/* Stats Cards */}
        {stats && (
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
            <div className="bg-white rounded-xl shadow-sm p-4">
              <div className="text-2xl font-bold text-gray-800">{stats.totalPending}</div>
              <div className="text-sm text-gray-500">Pending</div>
            </div>
            <div className="bg-white rounded-xl shadow-sm p-4">
              <div className="text-2xl font-bold text-blue-600">{stats.totalAmountFormatted}</div>
              <div className="text-sm text-gray-500">Total Amount</div>
            </div>
            <div className="bg-white rounded-xl shadow-sm p-4">
              <div className="text-2xl font-bold text-red-600">{stats.fraudAlerts}</div>
              <div className="text-sm text-gray-500">Fraud Alerts</div>
            </div>
            <div className="bg-white rounded-xl shadow-sm p-4">
              <div className="text-2xl font-bold text-yellow-600">{stats.highRiskCount}</div>
              <div className="text-sm text-gray-500">High Risk</div>
            </div>
          </div>
        )}

        {/* Filters */}
        <div className="bg-white rounded-xl shadow-sm p-4 mb-6">
          <div className="flex space-x-4">
            {[
              { id: 'all', label: 'All Pending', icon: '📋' },
              { id: 'fraud', label: 'Fraud Alerts', icon: '🚨' },
              { id: 'high-risk', label: 'High Risk', icon: '⚠️' }
            ].map((f) => (
              <button
                key={f.id}
                onClick={() => setFilter(f.id as any)}
                className={`px-4 py-2 rounded-lg text-sm font-medium ${
                  filter === f.id
                    ? 'bg-blue-100 text-blue-700'
                    : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                }`}
              >
                <span className="mr-1">{f.icon}</span> {f.label}
              </button>
            ))}
          </div>
        </div>

        {/* Authorizations List */}
        <div className="bg-white rounded-xl shadow-sm overflow-hidden">
          {authorizations.length === 0 ? (
            <div className="text-center py-12 text-gray-500">
              <svg className="w-12 h-12 mx-auto mb-4 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <p>No pending authorizations</p>
            </div>
          ) : (
            <div className="divide-y divide-gray-100">
              {authorizations.map((auth) => (
                <div key={auth.authId} className={`p-4 hover:bg-gray-50 ${auth.isFraudAlert ? 'bg-red-50' : ''}`}>
                  <div className="flex justify-between items-start">
                    <div className="flex-1">
                      <div className="flex items-center space-x-3">
                        <span className="text-lg font-semibold text-gray-800">{auth.merchantName}</span>
                        {auth.isFraudAlert && (
                          <span className="bg-red-600 text-white px-2 py-0.5 rounded text-xs font-bold animate-pulse">
                            FRAUD ALERT
                          </span>
                        )}
                        {getStatusBadge(auth.status)}
                      </div>
                      <div className="mt-1 text-sm text-gray-500">
                        <span>{auth.merchantCategory}</span>
                        <span className="mx-2">|</span>
                        <span>{auth.merchantCity}, {auth.merchantCountry}</span>
                      </div>
                      <div className="mt-2 flex items-center space-x-4 text-sm">
                        <span className="text-gray-600">
                          Card: {auth.maskedCardNumber}
                        </span>
                        <span className="text-gray-400">|</span>
                        <span className="text-gray-600">
                          {formatDateTime(auth.authTimestamp)}
                        </span>
                        <span className="text-gray-400">|</span>
                        {getRiskBadge(auth.riskScore)}
                      </div>
                    </div>
                    <div className="text-right">
                      <div className="text-xl font-bold text-gray-800">{auth.amountFormatted}</div>
                      <div className="text-xs text-gray-500">{auth.currency}</div>
                    </div>
                  </div>

                  {/* Actions */}
                  {auth.status === 'PENDING' && (
                    <div className="mt-4 flex justify-end space-x-3">
                      {!auth.isFraudAlert && (
                        <button
                          onClick={() => handleApprove(auth)}
                          disabled={actionLoading}
                          className="px-4 py-2 bg-green-600 text-white rounded-lg text-sm hover:bg-green-700 disabled:opacity-50"
                        >
                          Approve
                        </button>
                      )}
                      <button
                        onClick={() => { setSelectedAuth(auth); setShowDeclineModal(true); }}
                        disabled={actionLoading}
                        className="px-4 py-2 bg-red-600 text-white rounded-lg text-sm hover:bg-red-700 disabled:opacity-50"
                      >
                        Decline
                      </button>
                      {auth.isFraudAlert && (
                        <button
                          onClick={() => handleReportFraud(auth)}
                          disabled={actionLoading}
                          className="px-4 py-2 bg-purple-600 text-white rounded-lg text-sm hover:bg-purple-700 disabled:opacity-50"
                        >
                          Report Fraud & Block Card
                        </button>
                      )}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>

        {/* CICS Mapping */}
        <div className="mt-6 bg-white rounded-xl shadow-sm p-6">
          <h3 className="text-lg font-semibold text-gray-800 mb-4">CICS to REST Mapping</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="bg-gray-50 rounded-lg p-4">
              <h4 className="font-medium text-gray-700 mb-2">z/OS CICS (Legacy)</h4>
              <ul className="text-sm text-gray-600 space-y-1">
                <li>Transaction: COAUTH0C (Option 11)</li>
                <li>BMS Map: COAUTH00</li>
                <li>VSAM File: AUTHPEND</li>
                <li>MQ Queue: AUTH.PENDING.Q</li>
              </ul>
            </div>
            <div className="bg-blue-50 rounded-lg p-4">
              <h4 className="font-medium text-blue-700 mb-2">Cloud Native</h4>
              <ul className="text-sm text-blue-600 space-y-1">
                <li>GET /api/v1/authorizations/pending</li>
                <li>React: PendingAuthorizations.tsx</li>
                <li>PostgreSQL: pending_authorizations</li>
                <li>Kafka: authorization.events</li>
              </ul>
            </div>
          </div>
        </div>
      </main>

      {/* Decline Modal */}
      {showDeclineModal && selectedAuth && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl p-6 max-w-md w-full mx-4">
            <h3 className="text-lg font-semibold text-gray-800 mb-4">Decline Authorization</h3>
            <p className="text-gray-600 mb-4">
              Decline {selectedAuth.amountFormatted} at {selectedAuth.merchantName}?
            </p>
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-1">Reason *</label>
              <select
                value={declineReason}
                onChange={(e) => setDeclineReason(e.target.value)}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg"
              >
                <option value="">Select reason...</option>
                <option value="Unrecognized merchant">Unrecognized merchant</option>
                <option value="Suspected fraud">Suspected fraud</option>
                <option value="Wrong amount">Wrong amount</option>
                <option value="Card not present">Card not present</option>
                <option value="Other">Other</option>
              </select>
            </div>
            <div className="flex justify-end space-x-3">
              <button
                onClick={() => { setShowDeclineModal(false); setDeclineReason(''); }}
                className="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg"
              >
                Cancel
              </button>
              <button
                onClick={handleDecline}
                disabled={!declineReason || actionLoading}
                className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50"
              >
                Decline
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default PendingAuthorizationsPage;
