import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AddAdminRequest, AdminAccount } from '../models/admin.model';
import { MessageResponse } from '../../shared/models/login.model';
import { environment } from '../../../environments/environment';
import { API_ENDPOINTS } from '../../core/constants';

// Admins page of the super admin: list, add (invite email), resend the invite, enable / disable, delete
@Injectable({
  providedIn: 'root'
})
export class SuperAdminService {

  public apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) { }

  getAdmins(): Observable<AdminAccount[]> {
    return this.http.get<AdminAccount[]>(this.apiUrl + API_ENDPOINTS.SUPER_ADMIN_ADMINS);
  }

  addAdmin(admin: AddAdminRequest): Observable<AdminAccount> {
    return this.http.post<AdminAccount>(this.apiUrl + API_ENDPOINTS.SUPER_ADMIN_ADMINS, admin);
  }

  // a new set-password link, the older one stops working
  resendInvite(userId: number): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(this.apiUrl + API_ENDPOINTS.SUPER_ADMIN_RESEND_INVITE(userId), {});
  }

  deleteAdmin(userId: number): Observable<void> {
    return this.http.delete<void>(this.apiUrl + API_ENDPOINTS.SUPER_ADMIN_ADMINS + '/' + userId);
  }

  setEnabled(userId: number, enabled: boolean): Observable<AdminAccount> {
    return this.http.patch<AdminAccount>(this.apiUrl + API_ENDPOINTS.SUPER_ADMIN_ADMINS + '/' + userId + '/status', { enabled });
  }
}
