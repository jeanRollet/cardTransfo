import axios from 'axios';

const api = axios.create({
  baseURL: window.location.origin
});

const API_URL = '/api/v1/bill-payments';

export interface BillPayee {
  payeeId: number;
  customerId: number;
  payeeName: string;
  payeeType: string;
  payeeTypeName: string;
  payeeAccountNumber: string;
  nickname: string;
  isActive: boolean;
}

export interface BillPayment {
  paymentId: number;
  accountId: number;
  payeeId: number;
  payeeName: string;
  amount: number;
  amountFormatted: string;
  paymentDate: string;
  scheduledDate: string;
  status: string;
  statusName: string;
  confirmationNumber: string;
  memo: string;
  isRecurring: boolean;
  recurringFrequency: string;
  nextPaymentDate: string;
}

export interface CreatePaymentRequest {
  accountId: number;
  payeeId: number;
  amount: number;
  paymentDate: string;
  memo?: string;
  isRecurring?: boolean;
  recurringFrequency?: string;
}

export interface CreatePayeeRequest {
  customerId: number;
  payeeName: string;
  payeeType: string;
  payeeAccountNumber?: string;
  nickname?: string;
}

const getAuthHeader = () => {
  const token = localStorage.getItem('accessToken');
  return token ? { Authorization: `Bearer ${token}` } : {};
};

const billPaymentService = {
  // Payees
  async getPayeesByCustomer(customerId: number): Promise<BillPayee[]> {
    const response = await api.get<BillPayee[]>(
      `${API_URL}/payees/customer/${customerId}`,
      { headers: getAuthHeader() }
    );
    return response.data;
  },

  async createPayee(request: CreatePayeeRequest): Promise<BillPayee> {
    const response = await api.post<BillPayee>(
      `${API_URL}/payees`,
      request,
      { headers: getAuthHeader() }
    );
    return response.data;
  },

  async deletePayee(payeeId: number): Promise<void> {
    await api.delete(`${API_URL}/payees/${payeeId}`, {
      headers: getAuthHeader()
    });
  },

  // Payments
  async getPaymentsByAccount(accountId: number): Promise<BillPayment[]> {
    const response = await api.get<BillPayment[]>(
      `${API_URL}/account/${accountId}`,
      { headers: getAuthHeader() }
    );
    return response.data;
  },

  async getPaymentsByCustomer(customerId: number): Promise<BillPayment[]> {
    const response = await api.get<BillPayment[]>(
      `${API_URL}/customer/${customerId}`,
      { headers: getAuthHeader() }
    );
    return response.data;
  },

  async getScheduledPayments(customerId: number): Promise<BillPayment[]> {
    const response = await api.get<BillPayment[]>(
      `${API_URL}/scheduled/customer/${customerId}`,
      { headers: getAuthHeader() }
    );
    return response.data;
  },

  async createPayment(request: CreatePaymentRequest): Promise<BillPayment> {
    const response = await api.post<BillPayment>(
      API_URL,
      request,
      { headers: getAuthHeader() }
    );
    return response.data;
  },

  async cancelPayment(paymentId: number): Promise<void> {
    await api.post(`${API_URL}/${paymentId}/cancel`, {}, {
      headers: getAuthHeader()
    });
  }
};

export default billPaymentService;
