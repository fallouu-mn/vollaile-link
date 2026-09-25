import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs/operators';

export interface AdminLoginResponse {
  token: string;
  administratorId: number;
  phone: string;
  mustChangePassword: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private tokenKey = 'admin_token';
  private mustChangePasswordKey = 'admin_must_change_password';
  private apiUrl = '/api/admin/auth';

  constructor(private http: HttpClient, private router: Router) {}

  login(phone: string, password: string) {
    return this.http.post<AdminLoginResponse>(`${this.apiUrl}/login`, { phone, password })
      .pipe(
        tap(response => {
          if (response.token) {
            localStorage.setItem(this.tokenKey, response.token);
            localStorage.setItem(
              this.mustChangePasswordKey,
              String(response.mustChangePassword)
            );
          }
        })
      );
  }

  changePassword(currentPassword: string, newPassword: string) {
    return this.http.post<void>(`${this.apiUrl}/change-password`, {
      currentPassword,
      newPassword
    }).pipe(
      tap(() => {
        localStorage.setItem(this.mustChangePasswordKey, 'false');
      })
    );
  }

  logout() {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.mustChangePasswordKey);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  isLoggedIn(): boolean {
    return this.getToken() !== null;
  }

  mustChangePassword(): boolean {
    return localStorage.getItem(this.mustChangePasswordKey) === 'true';
  }
}