import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ActionResult, CopilotReply, CopilotRequest } from '../models/copilot.model';
import { environment } from '../../../environments/environment';
import { API_ENDPOINTS } from '../../core/constants';

// the logged-in chat (ai-service): ask, then Confirm / Cancel the cards it prepared
@Injectable({
  providedIn: 'root'
})
export class CopilotService {

  public apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) { }

  ask(request: CopilotRequest): Observable<CopilotReply> {
    return this.http.post<CopilotReply>(this.apiUrl + API_ENDPOINTS.COPILOT, request);
  }

  confirm(token: string): Observable<ActionResult> {
    return this.http.post<ActionResult>(this.apiUrl + API_ENDPOINTS.COPILOT_CONFIRM(token), {});
  }

  cancel(token: string): Observable<void> {
    return this.http.post<void>(this.apiUrl + API_ENDPOINTS.COPILOT_CANCEL(token), {});
  }

  clearMemory(chatId: string): Observable<void> {
    return this.http.delete<void>(this.apiUrl + API_ENDPOINTS.COPILOT_MEMORY + '/' + chatId);
  }
}
