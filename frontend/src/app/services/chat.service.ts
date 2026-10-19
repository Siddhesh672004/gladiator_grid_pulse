import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ChatRequest, ChatResponse } from '../models/chat.model';
import { environment } from '../../environments/environment';
import { API_ENDPOINTS } from '../constants';

// faq chatbot api calls, public (no login needed)
@Injectable({
  providedIn: 'root'
})
export class ChatService {

  public apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) { }

  sendMessage(request: ChatRequest): Observable<ChatResponse> {
    return this.http.post<ChatResponse>(this.apiUrl + API_ENDPOINTS.CHAT, request);
  }

  clearMemory(sessionId: string): Observable<void> {
    return this.http.delete<void>(this.apiUrl + API_ENDPOINTS.CHAT_MEMORY + '/' + sessionId);
  }
}
