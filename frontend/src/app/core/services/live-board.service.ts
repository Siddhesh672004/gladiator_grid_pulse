import { Injectable, NgZone } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { EMPTY, Observable, Subscription, catchError, filter, fromEvent, merge, switchMap, throwError, timer } from 'rxjs';
import { LiveStanding } from '../../shared/models/live-standing.model';
import { RaceControlService } from './race-control.service';
import { environment } from '../../../environments/environment';
import { API_ENDPOINTS, LIVE_REFRESH_MS } from '../constants';

// a live board pushed by the server (server-sent events): the whole board right away, then after every change
// a stream that can't be opened, or is closed for good -> the old polling every 4 s
@Injectable({
  providedIn: 'root'
})
export class LiveBoardService {

  constructor(private zone: NgZone, private raceControlService: RaceControlService) { }

  watch(sessionId: number): Observable<LiveStanding[]> {
    return new Observable<LiveStanding[]>(subscriber => {
      let polling: Subscription | undefined;
      const stream = new EventSource(environment.apiUrl + API_ENDPOINTS.LIVE_SESSION(sessionId));
      stream.addEventListener('standings', (event) =>
        this.zone.run(() => subscriber.next(JSON.parse((event as MessageEvent).data))));
      // a short break: the browser connects again by itself and gets the board again; closed: polling takes over
      stream.onerror = () => {
        if (stream.readyState === EventSource.CLOSED && !polling) {
          polling = this.poll(sessionId).subscribe(subscriber);
        }
      };
      return () => {
        stream.close();
        polling?.unsubscribe();
      };
    });
  }

  // a failed read is skipped (the next one comes 4 s later), an unknown session ends the board
  // a hidden tab skips the rounds and reads the board again when it is shown
  private poll(sessionId: number): Observable<LiveStanding[]> {
    return merge(timer(0, LIVE_REFRESH_MS), fromEvent(document, 'visibilitychange')).pipe(
      filter(() => !document.hidden),
      switchMap(() => this.raceControlService.getLiveStandings(sessionId).pipe(
        catchError((err: HttpErrorResponse) => err.status === 404 ? throwError(() => err) : EMPTY))));
  }
}
