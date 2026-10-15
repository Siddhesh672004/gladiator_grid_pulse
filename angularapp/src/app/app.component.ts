import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { NavigationEnd, NavigationError, RouteConfigLoadEnd, RouteConfigLoadStart, Router, RouterOutlet } from '@angular/router';
import { Subscription } from 'rxjs';
import { AdminnavComponent } from './components/adminnav/adminnav.component';
import { ChatbotComponent } from './components/chatbot/chatbot.component';
import { UsernavComponent } from './components/usernav/usernav.component';
import { AuthService } from './services/auth.service';
import { UserStorageService } from './services/user-storage.service';
import { BasicModule } from './shared/basic.module';

// landing, how it works and the login pages bring their own top bar
const PUBLIC_PAGES = ['/login', '/signup', '/admin-login', '/forgot-password', '/set-password', '/how-it-works'];

// the main shell -> the right navbar on top, the current page (router-outlet), and the chatbot on every page
// standalone: it lists what its template uses, there is no app.module
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, BasicModule, AdminnavComponent, UsernavComponent, ChatbotComponent],
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
