import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { jwtDecode } from 'jwt-decode';
import { STORAGE_KEYS } from '../constants';

// what is inside our jwt (JwtUtils.generateToken in spring)
export interface GridPulseToken {
  sub: string;        // email
  userId: number;
  username: string;
  userRole: string;
  iat: number;
  exp: number;        // seconds since 1970
}

// the only class that touches the logged-in user in localStorage (token, role, username, id - never a password)
@Injectable({
  providedIn: 'root'
})
export class UserStorageService {

  loggedIn$ = new BehaviorSubject<boolean>(this.getToken() !== null);

  saveLogin(token: string, username: string, userRole: string, userId: number): void {
    localStorage.setItem(STORAGE_KEYS.TOKEN, token);
    localStorage.setItem(STORAGE_KEYS.USERNAME, username);
    localStorage.setItem(STORAGE_KEYS.USER_ROLE, userRole);
    localStorage.setItem(STORAGE_KEYS.USER_ID, String(userId));
    this.loggedIn$.next(true);
  }

  clear(): void {
    localStorage.clear();
    this.loggedIn$.next(false);
  }

  getToken(): string | null {
    return localStorage.getItem(STORAGE_KEYS.TOKEN);
  }

  getUsername(): string | null {
    return localStorage.getItem(STORAGE_KEYS.USERNAME);
  }

  getUserId(): number | null {
    const id = localStorage.getItem(STORAGE_KEYS.USER_ID);
    return id ? Number(id) : null;
  }

  // the role comes from the token, so editing localStorage by hand changes nothing
  getUserRole(): string | null {
    return this.decodeToken()?.userRole ?? localStorage.getItem(STORAGE_KEYS.USER_ROLE);
  }

  // only reads the payload, the backend checks the signature
  decodeToken(): GridPulseToken | null {
    const token = this.getToken();
    if (!token) {
      return null;
    }
    try {
      return jwtDecode<GridPulseToken>(token);
    } catch {
      return null;
    }
  }

  isTokenExpired(): boolean {
    const decoded = this.decodeToken();
    return decoded !== null && typeof decoded.exp === 'number' && decoded.exp * 1000 < Date.now();
  }
}
