export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  role: string;
  state: boolean | null;
}

export interface RegisterRequest {
  email: string;
  fullName: string;
  password: string;
}

export interface RegisterResponse {
  email: string;
  fullName: string;
  role: string;
}
