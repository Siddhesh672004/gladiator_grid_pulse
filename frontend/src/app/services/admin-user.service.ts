import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AdminUser, PageResponse } from '../models/admin-user.model';
import { environment } from '../../environments/environment';
import { API_ENDPOINTS } from '../constants';

// Manage Users: the driver accounts, one page at a time
@Injectable({
  providedIn: 'root'
})
export class AdminUserService {

  public apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) { }

  getUsers(search: string, status: string, page: number, size: number): Observable<PageResponse<AdminUser>> {
    const params = new HttpParams()
      .set('search', search)
      .set('status', status)
      .set('page', page)
      .set('size', size);
    return this.http.get<PageResponse<AdminUser>>(this.apiUrl + API_ENDPOINTS.ADMIN_USERS, { params });
  }

  setEnabled(userId: number, enabled: boolean): Observable<AdminUser> {
    return this.http.patch<AdminUser>(this.apiUrl + API_ENDPOINTS.ADMIN_USER_STATUS(userId), { enabled });
  }
}
