import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { SignupRequest, User } from '../../shared/models/user.model';
import { Login, LoginResponse, MessageResponse, OtpChallenge } from '../../shared/models/login.model';
import { environment } from '../../../environments/environment';
import { ADMIN_ROLES, API_ENDPOINTS, ROLES } from '../constants';
import { UserStorageService } from './user-storage.service';

// sign up (code by email first), both logins, forgot password, logout + who is logged in
@Injectable({
  providedIn: 'root'
})
export class AuthService {

  public apiUrl = environment.apiUrl;

  constructor(private http: HttpClient, private userStorage: UserStorageService) { }

  // step 1: the backend emails a code, no account yet
  sendSignupCode(user: SignupRequest): Observable<OtpChallenge> {
    return this.http.post<OtpChallenge>(this.apiUrl + API_ENDPOINTS.REGISTER_SEND_CODE, user);
  }

  // step 2: same details + the code -> the account is made
  register(user: SignupRequest): Observable<User> {
    return this.http.post<User>(this.apiUrl + API_ENDPOINTS.REGISTER, user);
  }

  login(login: Login): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(this.apiUrl + API_ENDPOINTS.LOGIN, login).pipe(
      tap((res) => this.saveLogin(res))
    );
  }

  adminLogin(login: Login): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(this.apiUrl + API_ENDPOINTS.ADMIN_LOGIN, login).pipe(
      tap((res) => this.saveLogin(res))
    );
  }

  forgotPassword(email: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(this.apiUrl + API_ENDPOINTS.PASSWORD_FORGOT, { email });
  }

  resetPassword(email: string, code: string, newPassword: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(this.apiUrl + API_ENDPOINTS.PASSWORD_RESET, { email, code, newPassword });
  }

  // invite link of a new admin: works once
  setInitialPassword(email: string, token: string, newPassword: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(this.apiUrl + API_ENDPOINTS.PASSWORD_SET_INITIAL, { email, token, newPassword });
  }

  logout(): void {
    this.userStorage.clear();
  }

  isAuthenticated(): boolean {
    return this.userStorage.getToken() !== null;
  }

  isAdmin(): boolean {
    return ADMIN_ROLES.includes(this.getRole());
  }

  isSuperAdmin(): boolean {
    return this.getRole() === ROLES.SUPER_ADMIN;
  }

  isCustomer(): boolean {
    return this.getRole() === ROLES.USER;
  }

  getRole(): string {
    return this.userStorage.getUserRole() ?? '';
  }

  getCustomerName(): string {
    return this.userStorage.getUsername() ?? '';
  }

  private saveLogin(res: LoginResponse): void {
    this.userStorage.saveLogin(res.token, res.username, res.userRole, res.userId);
  }
}
