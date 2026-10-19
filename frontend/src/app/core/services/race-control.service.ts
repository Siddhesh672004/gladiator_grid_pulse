import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { LiveStanding } from '../../shared/models/live-standing.model';
import { environment } from '../../../environments/environment';
import { API_ENDPOINTS } from '../constants';

// live board calls by sessionId, the buttons are for practice sessions only
@Injectable({
  providedIn: 'root'
})
export class RaceControlService {

  public apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) { }

  // public, sent without a token
  getLiveStandings(sessionId: number): Observable<LiveStanding[]> {
    return this.http.get<LiveStanding[]>(this.apiUrl + API_ENDPOINTS.RACE_CONTROL_SESSION(sessionId));
  }

  // NOT_STARTED -> RACING, no lap is counted
  startRace(raceBookingId: number): Observable<LiveStanding> {
    return this.http.post<LiveStanding>(this.apiUrl + API_ENDPOINTS.RACE_CONTROL_START(raceBookingId), {});
  }

  // only for a RACING car
  incrementLap(raceBookingId: number): Observable<LiveStanding> {
    return this.http.post<LiveStanding>(this.apiUrl + API_ENDPOINTS.RACE_CONTROL_LAP(raceBookingId), {});
  }

  retireCar(raceBookingId: number): Observable<LiveStanding> {
    return this.http.post<LiveStanding>(this.apiUrl + API_ENDPOINTS.RACE_CONTROL_RETIRE(raceBookingId), {});
  }

  resetSession(sessionId: number): Observable<LiveStanding[]> {
    return this.http.post<LiveStanding[]>(this.apiUrl + API_ENDPOINTS.RACE_CONTROL_RESET(sessionId), {});
  }

  finishSession(sessionId: number): Observable<LiveStanding[]> {
    return this.http.post<LiveStanding[]>(this.apiUrl + API_ENDPOINTS.RACE_CONTROL_FINISH(sessionId), {});
  }
}
