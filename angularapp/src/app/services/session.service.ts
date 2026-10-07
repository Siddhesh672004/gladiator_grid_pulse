import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RaceSession, SessionRequest } from '../models/session.model';
import { environment } from '../../environments/environment';
import { API_ENDPOINTS } from '../constants';

// sessions: the admin schedules them, every page lists them
@Injectable({
  providedIn: 'root'
})
export class SessionService {

  public apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) { }

  getAllSessions(): Observable<RaceSession[]> {
    return this.http.get<RaceSession[]>(this.apiUrl + API_ENDPOINTS.SESSIONS);
  }

  getSessionById(sessionId: number): Observable<RaceSession> {
    return this.http.get<RaceSession>(this.apiUrl + API_ENDPOINTS.SESSIONS + '/' + sessionId);
  }

  // public, date = "yyyy-MM-dd"
  getSessionsForTrackOnDate(trackId: number, date: string): Observable<RaceSession[]> {
    const params = new HttpParams().set('date', date);
    return this.http.get<RaceSession[]>(this.apiUrl + API_ENDPOINTS.SESSIONS_BY_TRACK(trackId), { params });
  }

  addPracticeSession(request: SessionRequest): Observable<RaceSession> {
    return this.http.post<RaceSession>(this.apiUrl + API_ENDPOINTS.SESSIONS, request);
  }

  updatePracticeSession(sessionId: number, request: SessionRequest): Observable<RaceSession> {
    return this.http.put<RaceSession>(this.apiUrl + API_ENDPOINTS.SESSIONS + '/' + sessionId, request);
  }

  // its bookings are rejected and the drivers get an email
  cancelSession(sessionId: number): Observable<RaceSession> {
    return this.http.post<RaceSession>(this.apiUrl + API_ENDPOINTS.SESSION_CANCEL(sessionId), {});
  }
}
