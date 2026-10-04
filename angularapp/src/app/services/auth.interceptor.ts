import { Injectable } from '@angular/core';
import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, catchError, throwError } from 'rxjs';
import { ADMIN_ROLES, PUBLIC_ENDPOINTS } from '../constants';
import { UserStorageService } from './user-storage.service';

// every request passes here: the Bearer token is added, except for the public endpoints (same list as SecurityConfig)
// 401 -> logout + the right login page, 403 -> stay logged in, the page shows the message
@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  constructor(private userStorage: UserStorageService, private router: Router) { }

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const token = this.userStorage.getToken();
    const sendToken = token !== null && !this.isPublic(req);
    if (sendToken) {
      req = req.clone({ setHeaders: { Authorization: 'Bearer ' + token } });
    }
    return next.handle(req).pipe(
      catchError((err: HttpErrorResponse) => {
        if (sendToken && err.status === 401) {
          this.logoutToLoginPage();
        } 
        // the page that made the call still gets the error
        return throwError(() => err);
      })
    );
  }

  // the token is no longer accepted: forget the login and open the login page of that role
  private logoutToLoginPage(): void {
    const wasAdmin = ADMIN_ROLES.includes(this.userStorage.getUserRole() ?? '');
    this.userStorage.clear();
    if (wasAdmin) {
      this.router.navigate(['/admin-login']);
    } else {
      this.router.navigate(['/login']);
    }
  }

  // matches the whole path: GET /api/race-track is public, /api/race-booking/user/3 is not
  private isPublic(req: HttpRequest<any>): boolean {
    const path = req.url.split('?')[0];
    for (const rule of PUBLIC_ENDPOINTS) {
      const sameMethod = rule.method === '*' || rule.method === req.method;
      if (sameMethod && rule.pattern.test(path)) {
        return true;
      }
    }
    return false;
  }
}
