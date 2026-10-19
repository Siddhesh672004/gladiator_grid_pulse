import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TicketCheck } from '../models/ticket.model';
import { environment } from '../../../environments/environment';
import { API_ENDPOINTS } from '../../core/constants';

// admin: is this e-ticket code real, and may the car in? (the pdf itself: RaceService.downloadTicket)
@Injectable({
  providedIn: 'root'
})
export class TicketService {

  public apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) { }

  verify(code: string): Observable<TicketCheck> {
    return this.http.get<TicketCheck>(this.apiUrl + API_ENDPOINTS.TICKET_VERIFY(code));
  }
}
