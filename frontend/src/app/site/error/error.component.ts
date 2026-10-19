import { Component } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';

// shown for unknown urls and when a logged-in user opens a page of the other role
@Component({
  selector: 'app-error',
  templateUrl: './error.component.html'
})
export class ErrorComponent {

  isLoggedIn: boolean;

  constructor(private authService: AuthService) {
    this.isLoggedIn = this.authService.isAuthenticated();
  }
}
