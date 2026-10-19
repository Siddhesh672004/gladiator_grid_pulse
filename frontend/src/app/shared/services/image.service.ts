import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ImageResponse } from '../models/image.model';
import { environment } from '../../../environments/environment';
import { API_ENDPOINTS } from '../../core/constants';

// picture upload: file -> backend -> cloudinary -> the link comes back
// the page saves the link with its own form
@Injectable({
  providedIn: 'root'
})
export class ImageService {

  public apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) { }

  upload(kind: string, file: File): Observable<ImageResponse> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<ImageResponse>(this.apiUrl + API_ENDPOINTS.IMAGE_UPLOAD(kind), form);
  }
}
