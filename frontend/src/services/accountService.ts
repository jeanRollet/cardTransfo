import axios from 'axios';

// Create axios instance with baseURL from current origin
const api = axios.create({
  baseURL: window.location.origin
});

const API_URL = '/api/v1/accounts';

export interface AccountResponse {
  accountId: number;
  customerId: number;
  customerName: string;
  activeStatus: string;
  statusName: string;
  currentBalance: number;
  creditLimit: number;
  cashCreditLimit: number;
  availableCredit: number;
  utilizationRate: number;
  openDate: string;
  openDateFormatted: string;
  expiryDate: string;
  expiryDateFormatted: string;
  reissueDate: string | null;
  currCycleCredit: number;
  currCycleDebit: number;
  groupId: string;
  isActive: boolean;
  isExpired: boolean;
}

export interface CustomerResponse {
  customerId: number;
  firstName: string;
  lastName: string;
  fullName: string;
  dateOfBirth: string;
  dateOfBirthFormatted: string;
  ficoCreditScore: number;
  creditScoreRating: string;
  addressLine1: string;
  addressLine2: string;
  city: string;
  state: string;
  zipCode: string;
  fullAddress: string;
  phoneNumber: string;
  email: string;
}

export interface AccountSummaryResponse {
  totalAccounts: number;
  activeAccounts: number;
  closedAccounts: number;
  expiredAccounts: number;
  totalBalance: number;
  totalCreditLimit: number;
  totalAvailableCredit: number;
  averageUtilization: number;
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

const accountService = {
  async getAllAccounts(page = 0, size = 20): Promise<PageResponse<AccountResponse>> {
    const response = await api.get(`${API_URL}`, {
      params: { page, size },
      headers: getAuthHeader()
    });
    return response.data;
  },

  async getAccountById(accountId: number): Promise<AccountResponse> {
    const response = await api.get<AccountResponse>(`${API_URL}/${accountId}`, {
      headers: getAuthHeader()
    });
    return response.data;
  },

  async getAccountsByCustomer(customerId: number): Promise<AccountResponse[]> {
    const response = await api.get<AccountResponse[]>(
      `${API_URL}/customer/${customerId}`,
      { headers: getAuthHeader() }
    );
    return response.data;
  },

  async searchAccounts(name: string): Promise<AccountResponse[]> {
    const response = await api.get<AccountResponse[]>(`${API_URL}/search`, {
      params: { name },
      headers: getAuthHeader()
    });
    return response.data;
  },

  async getAccountSummary(): Promise<AccountSummaryResponse> {
    const response = await api.get<AccountSummaryResponse>(`${API_URL}/summary`, {
      headers: getAuthHeader()
    });
    return response.data;
  },

  async getExpiringAccounts(days = 30): Promise<AccountResponse[]> {
    const response = await api.get<AccountResponse[]>(`${API_URL}/expiring`, {
      params: { days },
      headers: getAuthHeader()
    });
    return response.data;
  },

  async getHighUtilizationAccounts(threshold = 80): Promise<AccountResponse[]> {
    const response = await api.get<AccountResponse[]>(`${API_URL}/high-utilization`, {
      params: { threshold },
      headers: getAuthHeader()
    });
    return response.data;
  },

  async updateAccountStatus(accountId: number, status: string): Promise<AccountResponse> {
    const response = await api.put<AccountResponse>(
      `${API_URL}/${accountId}/status`,
      { status },
      { headers: getAuthHeader() }
    );
    return response.data;
  },

  async getCustomerById(customerId: number): Promise<CustomerResponse> {
    const response = await api.get<CustomerResponse>(
      `${API_URL}/customers/${customerId}`,
      { headers: getAuthHeader() }
    );
    return response.data;
  },

  async getAllCustomers(page = 0, size = 20): Promise<PageResponse<CustomerResponse>> {
    const response = await api.get(`${API_URL}/customers`, {
      params: { page, size },
      headers: getAuthHeader()
    });
    return response.data;
  },

  async searchCustomers(name: string): Promise<CustomerResponse[]> {
    const response = await api.get<CustomerResponse[]>(`${API_URL}/customers/search`, {
      params: { name },
      headers: getAuthHeader()
    });
    return response.data;
  }
};

export default accountService;
