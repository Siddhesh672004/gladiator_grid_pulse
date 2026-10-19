import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AdminDashboard, LiveRace } from '../models/admin-dashboard.model';
import { environment } from '../../../environments/environment';
import { API_ENDPOINTS } from '../../core/constants';

// the admin dashboard's two calls
@Injectable({
  providedIn: 'root'
})
export class AdminDashboardService {

  public apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) { }

  getDashboard(): Observable<AdminDashboard> {
    return this.http.get<AdminDashboard>(this.apiUrl + API_ENDPOINTS.ADMIN_DASHBOARD);
  }

  getLiveRaces(): Observable<LiveRace[]> {
    return this.http.get<LiveRace[]>(this.apiUrl + API_ENDPOINTS.ADMIN_LIVE_RACES);
  }
}
