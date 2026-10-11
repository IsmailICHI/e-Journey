import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../models/page.model';
import { UserProfileResponse, UserResponse } from '../models/user.model';

export interface UserStatisticsResponse {
  totalUsers: number;
  activeUsers: number;
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly usersUrl = `${environment.apiUrl}/api/v1/admin/users`;

  getUsers(page = 0): Observable<PageResponse<UserResponse>> {
    return this.http.get<PageResponse<UserResponse>>(this.usersUrl, { params: { page } });
  }

  getUserStatistics(): Observable<UserStatisticsResponse> {
    return this.http.get<UserStatisticsResponse>(`${this.usersUrl}/statistics`);
  }

  getMyProfile(): Observable<UserProfileResponse> {
    return this.http.get<UserProfileResponse>(`${environment.apiUrl}/api/v1/users/me`);
  }

  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.usersUrl}/${id}`);
  }
}
