import axios from 'axios';

// Create axios instance with baseURL from current origin
const api = axios.create({
  baseURL: window.location.origin
});

const API_URL = '/api/v1/cards';

export interface CardResponse {
  cardNumber: string;
  lastFourDigits: string;
  accountId: number;
  cardType: string;
  cardTypeName: string;
  embossedName: string;
  expiryDate: string;
  expiryFormatted: string;
  status: string;
  statusName: string;
  issuedDate: string;
  isExpired: boolean;
  isActive: boolean;
}

export interface CardListResponse {
  accountId: number;
  cards: CardResponse[];
  totalCards: number;
  activeCards: number;
  expiredCards: number;
  blockedCards: number;
}

const getAuthHeader = () => {
  const token = localStorage.getItem('accessToken');
  return token ? { Authorization: `Bearer ${token}` } : {};
};

const cardService = {
  async getCardsByAccount(accountId: number): Promise<CardListResponse> {
    const response = await api.get<CardListResponse>(
      `${API_URL}/account/${accountId}`,
      { headers: getAuthHeader() }
    );
    return response.data;
  },

  async getAllCards(page = 0, size = 20): Promise<{ content: CardResponse[]; totalElements: number; totalPages: number }> {
    const response = await api.get(`${API_URL}`, {
      params: { page, size },
      headers: getAuthHeader()
    });
    return response.data;
  },

  async getCardDetails(cardNumber: string): Promise<CardResponse> {
    const response = await api.get<CardResponse>(`${API_URL}/${cardNumber}`, {
      headers: getAuthHeader()
    });
    return response.data;
  },

  async updateCardStatus(cardNumber: string, status: string, reason?: string): Promise<CardResponse> {
    const response = await api.put<CardResponse>(
      `${API_URL}/${cardNumber}/status`,
      { status, reason },
      { headers: getAuthHeader() }
    );
    return response.data;
  },

  async blockCard(cardNumber: string, reason: string): Promise<CardResponse> {
    const response = await api.post<CardResponse>(
      `${API_URL}/${cardNumber}/block`,
      { reason },
      { headers: getAuthHeader() }
    );
    return response.data;
  },

  async searchCards(name: string): Promise<CardResponse[]> {
    const response = await api.get<CardResponse[]>(`${API_URL}/search`, {
      params: { name },
      headers: getAuthHeader()
    });
    return response.data;
  },

  async getExpiringCards(days = 30): Promise<CardResponse[]> {
    const response = await api.get<CardResponse[]>(`${API_URL}/expiring`, {
      params: { days },
      headers: getAuthHeader()
    });
    return response.data;
  },

  async getCardsByCustomer(customerId: number): Promise<CardResponse[]> {
    const response = await api.get<CardResponse[]>(
      `${API_URL}/customer/${customerId}`,
      { headers: getAuthHeader() }
    );
    return response.data;
  }
};

export default cardService;
