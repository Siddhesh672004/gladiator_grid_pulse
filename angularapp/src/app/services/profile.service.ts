import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject, tap } from 'rxjs';
import { Profile, ProfileRequest } from '../models/profile.model';
import { environment } from '../../environments/environment';
import { API_ENDPOINTS } from '../constants';

// the profile of whoever is logged in, the user comes from the token
@Injectable({
  providedIn: 'root'
})
export class ProfileService {

  public apiUrl = environment.apiUrl;

  // the navbar icon listens: every profile that comes back updates the picture
  picture$ = new Subject<string | null>();

  constructor(private http: HttpClient) { }

  getProfile(): Observable<Profile> {
    return this.http.get<Profile>(this.apiUrl + API_ENDPOINTS.PROFILE).pipe(
      tap(profile => this.picture$.next(profile.profilePicture))
    );
  }

  // date of birth + license are locked after the first save (409), the picture can change any time
  updateProfile(request: ProfileRequest): Observable<Profile> {
    return this.http.put<Profile>(this.apiUrl + API_ENDPOINTS.PROFILE, request).pipe(
      tap(profile => this.picture$.next(profile.profilePicture))
    );
  }
}
