import { CommonModule } from '@angular/common';
import { Component, EventEmitter, OnDestroy, OnInit, Output } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { ProfileService } from '../../services/profile.service';
import { BasicModule } from '../../shared/basic.module';

// round icon in both navbars: Show User Profile -> /profile, Log Out -> tells the navbar, which owns the logout popup
@Component({
  selector: 'app-profilemenu',
  standalone: true,
  imports: [CommonModule, BasicModule],
  templateUrl: './profilemenu.component.html'
})
export class ProfilemenuComponent implements OnInit, OnDestroy {

  @Output() logoutClicked = new EventEmitter<void>();

  username: string = '';
  role: string = '';
  initial: string = '?';    // shown in the icon when there is no picture
  picture: string | null = null;
  menuOpen: boolean = false;

  private pictureSubscription?: Subscription;

  constructor(private authService: AuthService, private profileService: ProfileService, private router: Router) { }

  ngOnInit(): void {
    this.username = this.authService.getCustomerName();
    this.role = this.authService.getRole();
    this.initial = this.username.charAt(0).toUpperCase() || '?';
    // ProfileService announces the picture: after the call below, and whenever the profile page saves a new one
    this.pictureSubscription = this.profileService.picture$.subscribe(picture => this.picture = picture);
    this.profileService.getProfile().subscribe({
      error: () => this.picture = null
    });
  }

  ngOnDestroy(): void {
    this.pictureSubscription?.unsubscribe();
  }

  showProfile(): void {
    this.menuOpen = false;
    this.router.navigate(['/profile']);
  }

  logOut(): void {
    this.menuOpen = false;
    this.logoutClicked.emit();
  }
}
