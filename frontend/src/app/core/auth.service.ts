import { HttpClient } from '@angular/common/http';
import { Injectable, computed, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { AuthResponse, UserSummary } from './models';

const TOKEN_KEY = 'bookdb.token';
const USER_KEY = 'bookdb.user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly tokenSignal = signal<string | null>(localStorage.getItem(TOKEN_KEY));
  private readonly userSignal = signal<UserSummary | null>(this.readStoredUser());

  readonly token = computed(() => this.tokenSignal());
  readonly currentUser = computed(() => this.userSignal());
  readonly isLoggedIn = computed(() => this.tokenSignal() !== null);
  readonly isAdmin = computed(() => this.userSignal()?.role === 'ADMIN');

  constructor(private readonly http: HttpClient) {}

  register(email: string, password: string, displayName: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/auth/register', { email, password, displayName })
      .pipe(tap((res) => this.applySession(res)));
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/auth/login', { email, password })
      .pipe(tap((res) => this.applySession(res)));
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.tokenSignal.set(null);
    this.userSignal.set(null);
  }

  private applySession(res: AuthResponse): void {
    localStorage.setItem(TOKEN_KEY, res.token);
    localStorage.setItem(USER_KEY, JSON.stringify(res.user));
    this.tokenSignal.set(res.token);
    this.userSignal.set(res.user);
  }

  private readStoredUser(): UserSummary | null {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? (JSON.parse(raw) as UserSummary) : null;
  }
}
