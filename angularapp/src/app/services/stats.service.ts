import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DriverStats, LeaderboardEntry } from '../models/stats.model';
import { environment } from '../../environments/environment';
import { API_ENDPOINTS } from '../constants';

// driver stats page + the leaderboard of the start page (public)
@Injectable({
  providedIn: 'root'
})
export class StatsService {

  public apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) { }

  getMyStats(): Observable<DriverStats> {
    return this.http.get<DriverStats>(this.apiUrl + API_ENDPOINTS.STATS_ME);
  }

  // limit 1 - 20
  getLeaderboard(limit: number): Observable<LeaderboardEntry[]> {
    const params = new HttpParams().set('limit', limit);
    return this.http.get<LeaderboardEntry[]>(this.apiUrl + API_ENDPOINTS.STATS_LEADERBOARD, { params });
  }
}
