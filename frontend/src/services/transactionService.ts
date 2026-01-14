import axios from 'axios';

// Create axios instance with baseURL from current origin
const api = axios.create({
  baseURL: window.location.origin
});

const API_URL = '/api/v1/transactions';

export interface TransactionResponse {
  transactionId: number;
  accountId: number;
  transactionType: string;
  transactionTypeName: string;
  transactionCategory: string;
  categoryName: string;
  transactionSource: string;
  sourceName: string;
  transactionDesc: string;
  transactionAmount: number;
  amountFormatted: string;
  merchantId: string;
  merchantName: string;
  merchantCity: string;
  merchantZip: string;
  merchantLocation: string;
  cardNumber: string;
  originalTranId: string;
  transactionDate: string;
  transactionTime: string;
  transactionDateTime: string;
  isDebit: boolean;
  isCredit: boolean;
}

export interface TransactionListResponse {
  accountId: number;
  transactions: TransactionResponse[];
  totalTransactions: number;
  creditCount: number;
  debitCount: number;
  totalCredits: number;
  totalDebits: number;
  netAmount: number;
  totalCreditsFormatted: string;
  totalDebitsFormatted: string;
  netAmountFormatted: string;
}

export interface CategorySummary {
  category: string;
  categoryName: string;
  transactionCount: number;
  totalAmount: number;
  totalAmountFormatted: string;
  percentage: number;
}

export interface TransactionSummaryResponse {
  accountId: number;
  customerId: number;
  totalTransactions: number;
  purchaseCount: number;
  paymentCount: number;
  refundCount: number;
  totalAmount: number;
  totalPurchases: number;
  totalPayments: number;
  totalRefunds: number;
  averageTransaction: number;
  totalAmountFormatted: string;
  totalPurchasesFormatted: string;
  totalPaymentsFormatted: string;
  averageTransactionFormatted: string;
  categoryBreakdown: CategorySummary[];
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

const getAuthHeader = () => {
  const token = localStorage.getItem('accessToken');
  return token ? { Authorization: `Bearer ${token}` } : {};
};

const transactionService = {
  async getAllTransactions(page = 0, size = 20): Promise<PageResponse<TransactionResponse>> {
    const response = await api.get(`${API_URL}`, {
      params: { page, size },
      headers: getAuthHeader()
    });
    return response.data;
  },

  async getTransactionsByAccount(accountId: number): Promise<TransactionListResponse> {
    const response = await api.get<TransactionListResponse>(
      `${API_URL}/account/${accountId}`,
      { headers: getAuthHeader() }
    );
    return response.data;
  },

  async getTransactionsByCustomer(customerId: number): Promise<TransactionResponse[]> {
    const response = await api.get<TransactionResponse[]>(
      `${API_URL}/customer/${customerId}`,
      { headers: getAuthHeader() }
    );
    return response.data;
  },

  async getTransactionById(transactionId: number): Promise<TransactionResponse> {
    const response = await api.get<TransactionResponse>(
      `${API_URL}/${transactionId}`,
      { headers: getAuthHeader() }
    );
    return response.data;
  },

  async getTransactionsByDateRange(startDate: string, endDate: string): Promise<TransactionResponse[]> {
    const response = await api.get<TransactionResponse[]>(`${API_URL}/date-range`, {
      params: { startDate, endDate },
      headers: getAuthHeader()
    });
    return response.data;
  },

  async searchTransactions(term: string): Promise<TransactionResponse[]> {
    const response = await api.get<TransactionResponse[]>(`${API_URL}/search`, {
      params: { term },
      headers: getAuthHeader()
    });
    return response.data;
  },

  async getAccountSummary(accountId: number): Promise<TransactionSummaryResponse> {
    const response = await api.get<TransactionSummaryResponse>(
      `${API_URL}/account/${accountId}/summary`,
      { headers: getAuthHeader() }
    );
    return response.data;
  },

  async getOverallSummary(): Promise<TransactionSummaryResponse> {
    const response = await api.get<TransactionSummaryResponse>(
      `${API_URL}/summary`,
      { headers: getAuthHeader() }
    );
    return response.data;
  },

  async getTransactionsByCard(cardNumber: string): Promise<TransactionResponse[]> {
    const response = await api.get<TransactionResponse[]>(
      `${API_URL}/card/${cardNumber}`,
      { headers: getAuthHeader() }
    );
    return response.data;
  },

  async createTransaction(request: CreateTransactionRequest): Promise<TransactionResponse> {
    const response = await api.post<TransactionResponse>(
      API_URL,
      request,
      { headers: getAuthHeader() }
    );
    return response.data;
  }
};

export interface CreateTransactionRequest {
  accountId: number;
  transactionType: string;
  transactionCategory: string;
  transactionSource?: string;
  transactionDesc?: string;
  amount: number;
  merchantName?: string;
  merchantCity?: string;
  merchantZip?: string;
  cardNumber?: string;
}

export default transactionService;
