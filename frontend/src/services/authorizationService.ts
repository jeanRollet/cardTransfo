import axios from 'axios';

const api = axios.create({
  baseURL: window.location.origin
});

const API_URL = '/api/v1/authorizations';

export interface PendingAuthorization {
  authId: number;
  accountId: number;
  cardNumber: string;
  maskedCardNumber: string;
  merchantName: string;
  merchantCategory: string;
  merchantCity: string;
  merchantCountry: string;
  amount: number;
  amountFormatted: string;
  currency: string;
  authCode: string;
  status: string;
  statusName: string;
  declineReason: string;
  riskScore: number;
  isFraudAlert: boolean;
  authTimestamp: string;
  expiryTimestamp: string;
  settledAt: string;
}

export interface AuthorizationStats {
  totalPending: number;
  totalAmount: number;
  totalAmountFormatted: string;
  fraudAlerts: number;
  highRiskCount: number;
}

const getAuthHeader = () => {
  const token = localStorage.getItem('accessToken');
  return token ? { Authorization: `Bearer ${token}` } : {};
};

const authorizationService = {
  async getPendingByCustomer(customerId: number): Promise<PendingAuthorization[]> {
    const response = await api.get<PendingAuthorization[]>(
      `${API_URL}/pending/customer/${customerId}`,
      { headers: getAuthHeader() }
    );
    return response.data;
  },

  async getPendingByAccount(accountId: number): Promise<PendingAuthorization[]> {
    const response = await api.get<PendingAuthorization[]>(
      `${API_URL}/pending/account/${accountId}`,
      { headers: getAuthHeader() }
    );
    return response.data;
  },

  async getAllPending(): Promise<PendingAuthorization[]> {
    const response = await api.get<PendingAuthorization[]>(
      `${API_URL}/pending`,
      { headers: getAuthHeader() }
    );
    return response.data;
  },

  async getFraudAlerts(): Promise<PendingAuthorization[]> {
    const response = await api.get<PendingAuthorization[]>(
      `${API_URL}/fraud-alerts`,
      { headers: getAuthHeader() }
    );
    return response.data;
  },

  async getStats(customerId?: number): Promise<AuthorizationStats> {
    const url = customerId
      ? `${API_URL}/stats/customer/${customerId}`
      : `${API_URL}/stats`;
    const response = await api.get<AuthorizationStats>(url, {
      headers: getAuthHeader()
    });
    return response.data;
  },

  async approveAuthorization(authId: number): Promise<PendingAuthorization> {
    const response = await api.post<PendingAuthorization>(
      `${API_URL}/${authId}/approve`,
      {},
      { headers: getAuthHeader() }
    );
    return response.data;
  },

  async declineAuthorization(authId: number, reason: string): Promise<PendingAuthorization> {
    const response = await api.post<PendingAuthorization>(
      `${API_URL}/${authId}/decline`,
      { reason },
      { headers: getAuthHeader() }
    );
    return response.data;
  },

  async reportFraud(authId: number): Promise<PendingAuthorization> {
    const response = await api.post<PendingAuthorization>(
      `${API_URL}/${authId}/report-fraud`,
      {},
      { headers: getAuthHeader() }
    );
    return response.data;
  }
};

export default authorizationService;
