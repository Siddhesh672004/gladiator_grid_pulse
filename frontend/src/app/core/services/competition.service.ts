import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Competition, CompetitionRequest, CompetitionRule } from '../../shared/models/competition.model';
import { LiveStanding } from '../../shared/models/live-standing.model';
import { environment } from '../../../environments/environment';
import { API_ENDPOINTS } from '../constants';

// competition calls: create / list / cancel + the Competition Control buttons
@Injectable({
  providedIn: 'root'
})
export class CompetitionService {

  public apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) { }

  createCompetition(request: CompetitionRequest): Observable<Competition> {
    return this.http.post<Competition>(this.apiUrl + API_ENDPOINTS.COMPETITIONS, request);
  }

  getAllCompetitions(): Observable<Competition[]> {
    return this.http.get<Competition[]>(this.apiUrl + API_ENDPOINTS.COMPETITIONS);
  }

  getCompetitionById(competitionId: number): Observable<Competition> {
    return this.http.get<Competition>(this.apiUrl + API_ENDPOINTS.COMPETITIONS + '/' + competitionId);
  }

  cancelCompetition(competitionId: number): Observable<Competition> {
    return this.http.post<Competition>(this.apiUrl + API_ENDPOINTS.COMPETITION_CANCEL(competitionId), {});
  }

  // laps + max cars a competition on this track would get
  getRuleForTrack(trackId: number): Observable<CompetitionRule> {
    return this.http.get<CompetitionRule>(this.apiUrl + API_ENDPOINTS.COMPETITION_RULE(trackId));
  }

  startQualifying(competitionId: number): Observable<Competition> {
    return this.runStep(competitionId, 'qualifying/start');
  }

  // the qualifying result becomes the starting grid of the race
  finishQualifying(competitionId: number): Observable<Competition> {
    return this.runStep(competitionId, 'qualifying/finish');
  }

  startRace(competitionId: number): Observable<Competition> {
    return this.runStep(competitionId, 'race/start');
  }

  finishRace(competitionId: number): Observable<Competition> {
    return this.runStep(competitionId, 'race/finish');
  }

  addLap(raceBookingId: number): Observable<LiveStanding> {
    return this.http.post<LiveStanding>(this.apiUrl + API_ENDPOINTS.COMPETITION_LAP(raceBookingId), {});
  }

  retireCar(raceBookingId: number): Observable<LiveStanding> {
    return this.http.post<LiveStanding>(this.apiUrl + API_ENDPOINTS.COMPETITION_RETIRE(raceBookingId), {});
  }

  private runStep(competitionId: number, step: string): Observable<Competition> {
    return this.http.post<Competition>(this.apiUrl + API_ENDPOINTS.COMPETITION_STEP(competitionId, step), {});
  }
}
