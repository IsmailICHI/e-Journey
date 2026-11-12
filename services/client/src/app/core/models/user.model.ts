export interface UserResponse {
  id: number;
  email: string;
  fullName: string;
  role: string;
  state: boolean | null;
}

export interface UserProfileResponse {
  fullName: string;
  email: string;
  role: string;
  state: boolean | null;
}

export interface PublicManagerResponse {
  id: number;
  fullName: string;
}
