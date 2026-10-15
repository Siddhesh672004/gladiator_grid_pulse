import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AddAdminRequest, AdminAccount } from '../models/admin.model';
import { environment } from '../../environments/environment';
import { API_ENDPOINTS } from '../constants';

// Admins page of the super admin: list, add, enable / disable, delete
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

  deleteAdmin(userId: number): Observable<void> {
    return this.http.delete<void>(this.apiUrl + API_ENDPOINTS.SUPER_ADMIN_ADMINS + '/' + userId);
  }

  setEnabled(userId: number, enabled: boolean): Observable<AdminAccount> {
    return this.http.patch<AdminAccount>(this.apiUrl + API_ENDPOINTS.SUPER_ADMIN_ADMINS + '/' + userId + '/status', { enabled });
  }
}
