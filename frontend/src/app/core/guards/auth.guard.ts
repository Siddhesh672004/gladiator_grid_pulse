import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, CanActivateChild, Router, UrlTree } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { UserStorageService } from '../services/user-storage.service';
import { ROLES } from '../constants';

// runs before a protected page opens: not logged in -> login page (admin pages -> admin login), wrong role -> error page
// the allowed roles come from the route data, a child route without its own roles uses the parent's
@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate, CanActivateChild {

  constructor(private authService: AuthService, private userStorage: UserStorageService, private router: Router) { }

  canActivate(route: ActivatedRouteSnapshot): boolean | UrlTree {
    const roles: string[] | undefined = route.data['roles'];
    if (!this.authService.isAuthenticated() || this.userStorage.isTokenExpired()) {
      this.authService.logout();
      // an admin page sends to the admin login, every other page to the driver login
      if (roles && roles.includes(ROLES.ADMIN)) {
        return this.router.parseUrl('/admin-login');
      }
      return this.router.parseUrl('/login');
    }
    if (roles && !roles.includes(this.authService.getRole())) {
      return this.router.parseUrl('/error');
    }
    return true;
  }

  canActivateChild(childRoute: ActivatedRouteSnapshot): boolean | UrlTree {
    return this.canActivate(childRoute);
  }
}
