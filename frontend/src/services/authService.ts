import axios from 'axios';

const API_URL = '/api/v1/auth';

export interface LoginRequest {
  userId: string;
  password: string;
}

export interface UserInfo {
  userId: string;
  firstName: string;
  lastName: string;
  userType: string;      // 'A' = Admin, 'U' = User
  customerId: number | null;  // null for admins
  sessionId: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  user: UserInfo;
}

export interface ValidateResponse {
  valid: boolean;
  message: string;
}

const authService = {
  async login(credentials: LoginRequest): Promise<LoginResponse> {
    const response = await axios.post<LoginResponse>(`${API_URL}/login`, credentials);
    if (response.data.accessToken) {
      localStorage.setItem('accessToken', response.data.accessToken);
      localStorage.setItem('refreshToken', response.data.refreshToken);
      localStorage.setItem('user', JSON.stringify(response.data.user));
    }
    return response.data;
  },

  async logout(): Promise<void> {
    const token = localStorage.getItem('accessToken');
    if (token) {
      try {
        await axios.post(`${API_URL}/logout`, {}, {
          headers: { Authorization: `Bearer ${token}` }
        });
      } catch (error) {
        console.error('Logout error:', error);
      }
    }
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
  },

  async validateToken(): Promise<boolean> {
    const token = localStorage.getItem('accessToken');
    if (!token) return false;

    try {
      const response = await axios.get<ValidateResponse>(`${API_URL}/validate`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      return response.data.valid;
    } catch {
      return false;
    }
  },

  getUser(): UserInfo | null {
    const user = localStorage.getItem('user');
    return user ? JSON.parse(user) : null;
  },

  getToken(): string | null {
    return localStorage.getItem('accessToken');
  },

  isAuthenticated(): boolean {
    return !!localStorage.getItem('accessToken');
  }
};

export default authService;
