import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, catchError, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse, LoginRequest, RegisterRequest, RegisterResponse } from '../models/auth.model';
import { UserProfileResponse } from '../models/user.model';

const TOKEN_KEY = 'e-journey.token';

type NormalizedRole = 'ADMIN' | 'TRAVEL_MANAGER' | 'TRAVELER' | 'USER';

interface TokenClaims {
  sub: string;
  exp: number;
  userId: number;
  role: NormalizedRole;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly verifiedToken = signal<string | null>(null);

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/api/v1/auth/login`, request).pipe(
      tap((response) => {
        const claims = this.readClaims(response.token);
        const responseRole = normalizeRole(response.role);
        if (!claims || claims.role !== responseRole || claims.sub.toLowerCase() !== request.email.toLowerCase()) {
          throw new Error('The authentication service returned an invalid or incomplete JWT.');
        }
        localStorage.setItem(TOKEN_KEY, response.token);
        sessionStorage.removeItem(TOKEN_KEY);
        this.verifiedToken.set(response.token);
      })
    );
  }

  register(request: RegisterRequest): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>(`${environment.apiUrl}/api/v1/auth/register`, request);
  }

  verifySession(): Observable<UserProfileResponse> {
    const token = this.token;
    return this.http.get<UserProfileResponse>(`${environment.apiUrl}/api/v1/users/me`).pipe(
      tap((profile) => {
        const claims = this.readClaims(token);
        const profileRole = normalizeRole(profile.role);
        if (!claims || claims.sub.toLowerCase() !== profile.email.toLowerCase() || !profileRole || claims.role !== profileRole) {
          this.logout();
          throw new Error('The session does not match the authenticated account.');
        }
        this.verifiedToken.set(token);
      }),
      catchError((error: unknown) => {
        if (error instanceof HttpErrorResponse && error.status === 401) {
          this.logout();
        }
        return throwError(() => error);
      })
    );
  }

  get token(): string | null {
    const token = localStorage.getItem(TOKEN_KEY);
    if (token) {
      return token;
    }
    const legacyToken = sessionStorage.getItem(TOKEN_KEY);
    if (legacyToken) {
      localStorage.setItem(TOKEN_KEY, legacyToken);
      sessionStorage.removeItem(TOKEN_KEY);
    }
    return legacyToken;
  }

  get email(): string | null {
    return this.claims?.sub ?? null;
  }

  get role(): string | null {
    return this.claims?.role ?? null;
  }

  get userId(): number | null {
    return this.claims?.userId ?? null;
  }

  get isLoggedIn(): boolean {
    return this.claims !== null;
  }

  get isSessionVerified(): boolean {
    return this.isLoggedIn && this.verifiedToken() === this.token;
  }

  get isAdmin(): boolean {
    return this.claims?.role === 'ADMIN';
  }

  get isTravelManager(): boolean {
    return this.claims?.role === 'TRAVEL_MANAGER';
  }

  get isTraveler(): boolean {
    return this.claims?.role === 'TRAVELER' || this.claims?.role === 'USER';
  }

  logout(): void {
    this.verifiedToken.set(null);
    localStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(TOKEN_KEY);
  }

  private get claims(): TokenClaims | null {
    return this.readClaims(this.token);
  }

  private readClaims(token: string | null): TokenClaims | null {
    if (!token) {
      return null;
    }
    const parts = token.split('.');
    if (parts.length !== 3 || !parts[2]) {
      return null;
    }

    try {
      const encodedPayload = parts[1].replace(/-/g, '+').replace(/_/g, '/');
      const paddedPayload = encodedPayload.padEnd(Math.ceil(encodedPayload.length / 4) * 4, '=');
      const binaryPayload = atob(paddedPayload);
      const payloadBytes = Uint8Array.from(binaryPayload, (character) => character.charCodeAt(0));
      const payload: unknown = JSON.parse(new TextDecoder().decode(payloadBytes));
      if (typeof payload !== 'object' || payload === null) {
        return null;
      }
      const claims = payload as Record<string, unknown>;
      const role = typeof claims['role'] === 'string'
        ? normalizeRole(claims['role'])
        : '';
      const userId = Number(claims['userId']);
      if (
        typeof claims['sub'] !== 'string'
        || claims['sub'].trim().length === 0
        || typeof claims['exp'] !== 'number'
        || claims['exp'] * 1000 <= Date.now()
        || !Number.isSafeInteger(userId)
        || userId <= 0
        || !isSupportedRole(role)
      ) {
        return null;
      }
      return { sub: claims['sub'], exp: claims['exp'], userId, role };
    } catch {
      return null;
    }
  }
}

function normalizeRole(value: string): NormalizedRole | '' {
  const withoutPrefix = value.trim().toUpperCase().replace(/^ROLE_/, '');
  const normalized = withoutPrefix
    .replace(/[^A-Z0-9]+/g, '_')
    .replace(/_+/g, '_')
    .replace(/^_|_$/g, '');

  const canonical = normalized
    .replace('TRAVELMANAGER', 'TRAVEL_MANAGER')
    .replace('TRAVELERUSER', 'TRAVELER');

  if (canonical === 'ADMIN' || canonical === 'TRAVEL_MANAGER' || canonical === 'TRAVELER' || canonical === 'USER') {
    return canonical as NormalizedRole;
  }
  return '';
}

function isSupportedRole(value: string): value is NormalizedRole {
  return value === 'ADMIN' || value === 'TRAVEL_MANAGER' || value === 'TRAVELER' || value === 'USER';
}
