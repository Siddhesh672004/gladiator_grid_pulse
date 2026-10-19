import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { NavigationEnd, NavigationError, RouteConfigLoadEnd, RouteConfigLoadStart, Router, RouterOutlet } from '@angular/router';
import { Subscription } from 'rxjs';
import { AdminNavComponent } from './core/layout/admin-nav/admin-nav.component';
import { ChatLauncherComponent } from './chat/chat-launcher/chat-launcher.component';
import { LoaderComponent } from './shared/ui/loader/loader.component';
import { UserNavComponent } from './core/layout/user-nav/user-nav.component';
import { AuthService } from './core/services/auth.service';
import { UserStorageService } from './core/services/user-storage.service';

// landing, how it works and the login pages bring their own top bar
const PUBLIC_PAGES = ['/login', '/signup', '/admin-login', '/forgot-password', '/set-password', '/how-it-works'];

// the main shell -> the right navbar on top, the current page (router-outlet), and the chat button on every page
// standalone: it lists what its template uses, there is no app.module
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, LoaderComponent, AdminNavComponent, UserNavComponent, ChatLauncherComponent],
  templateUrl: './app.component.html'
})
export class AppComponent implements OnInit, OnDestroy {

  isAdmin: boolean = false;
  isUser: boolean = false;
  onPublicPage: boolean = true;
  loading: boolean = false;       // true while a lazy module (site / admin / user pages) is being downloaded

  private subscriptions = new Subscription();

  constructor(private authService: AuthService, private userStorage: UserStorageService, private router: Router) { }

  ngOnInit(): void {
    this.subscriptions.add(this.userStorage.loggedIn$.subscribe(() => this.refreshRole()));
    this.subscriptions.add(this.router.events.subscribe(event => {
      if (event instanceof RouteConfigLoadStart) {
        this.loading = true;
      }
      if (event instanceof RouteConfigLoadEnd || event instanceof NavigationError) {
        this.loading = false;
      }
      if (event instanceof NavigationEnd) {
        this.pageOpened(event.urlAfterRedirects);
      }
    }));
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  private pageOpened(url: string): void {
    const path = url.split(/[?#]/)[0];
    this.onPublicPage = path === '/' || PUBLIC_PAGES.some(page => path.startsWith(page));
    this.refreshRole();
  }

  private refreshRole(): void {
    const loggedIn = this.authService.isAuthenticated();
    this.isAdmin = loggedIn && this.authService.isAdmin();
    this.isUser = loggedIn && this.authService.isCustomer();
  }
}
