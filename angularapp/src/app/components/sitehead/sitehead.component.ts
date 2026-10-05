import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { AuthService } from '../../services/auth.service';

// a section of the page the top bar can jump to
export interface SiteSection {
  id: string;
  label: string;
}

// top bar of the public pages: logo, the page's sections, Login / Sign up (or the way back in)
// use: <app-sitehead [sections]="sections" [current]="current" (jump)="goTo($event)"></app-sitehead>
@Component({
  selector: 'app-sitehead',
  templateUrl: './sitehead.component.html'
})
export class SiteheadComponent implements OnInit {

  @Input() sections: SiteSection[] = [];
  @Input() current: string = '';       // id of the section on screen
  @Output() jump = new EventEmitter<string>();

  loggedIn: boolean = false;
  homeLink: string = '/home';        // where "Open my dashboard" goes

  constructor(private authService: AuthService) { }

  ngOnInit(): void {
    this.loggedIn = this.authService.isAuthenticated();
    if (this.authService.isAdmin()) {
      this.homeLink = '/admin/dashboard';
    }
  }
}
