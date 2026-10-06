import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RaceTrack } from '../models/race-track.model';
import { BookingStatusRequest, CarCategory, RaceBooking } from '../models/race-booking.model';
import { environment } from '../../environments/environment';
import { API_ENDPOINTS } from '../constants';

// tracks + bookings (sessions: SessionService, competitions: CompetitionService)
@Injectable({
  providedIn: 'root'
})
export class RaceService {

  public apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) { }

  getAllTracks(): Observable<RaceTrack[]> {
    return this.http.get<RaceTrack[]>(this.apiUrl + API_ENDPOINTS.RACE_TRACK);
  }

  getTrackById(trackId: number): Observable<RaceTrack> {
    return this.http.get<RaceTrack>(this.apiUrl + API_ENDPOINTS.RACE_TRACK + '/' + trackId);
  }

  addTrack(track: RaceTrack): Observable<RaceTrack> {
    return this.http.post<RaceTrack>(this.apiUrl + API_ENDPOINTS.RACE_TRACK, track);
  }

  updateTrack(trackId: number, track: RaceTrack): Observable<RaceTrack> {
    return this.http.put<RaceTrack>(this.apiUrl + API_ENDPOINTS.RACE_TRACK + '/' + trackId, track);
  }

  // only a track that never had a session (409 otherwise -> deactivate it)
  deleteTrack(trackId: number): Observable<void> {
    return this.http.delete<void>(this.apiUrl + API_ENDPOINTS.RACE_TRACK + '/' + trackId);
  }

  deactivateTrack(trackId: number): Observable<RaceTrack> {
    return this.http.post<RaceTrack>(this.apiUrl + API_ENDPOINTS.RACE_TRACK_DEACTIVATE(trackId), {});
  }

  activateTrack(trackId: number): Observable<RaceTrack> {
    return this.http.post<RaceTrack>(this.apiUrl + API_ENDPOINTS.RACE_TRACK_ACTIVATE(trackId), {});
  }

  getCarCategories(): Observable<CarCategory[]> {
    return this.http.get<CarCategory[]>(this.apiUrl + API_ENDPOINTS.RACE_BOOKING_CAR_CATEGORIES);
  }

  getAllRaceBookings(): Observable<RaceBooking[]> {
    return this.http.get<RaceBooking[]>(this.apiUrl + API_ENDPOINTS.RACE_BOOKING);
  }

  getRaceBookingsByUserId(userId: number): Observable<RaceBooking[]> {
    return this.http.get<RaceBooking[]>(this.apiUrl + API_ENDPOINTS.RACE_BOOKING_BY_USER + '/' + userId);
  }

  updateRaceBooking(requestId: number, request: BookingStatusRequest): Observable<RaceBooking> {
    return this.http.put<RaceBooking>(this.apiUrl + API_ENDPOINTS.RACE_BOOKING + '/' + requestId, request);
  }

  deleteRaceBooking(requestId: number): Observable<void> {
    return this.http.delete<void>(this.apiUrl + API_ENDPOINTS.RACE_BOOKING + '/' + requestId);
  }
}
