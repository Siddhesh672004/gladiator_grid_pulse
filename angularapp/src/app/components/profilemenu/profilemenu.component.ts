import { CommonModule } from '@angular/common';
import { Component, ElementRef, EventEmitter, HostListener, OnDestroy, OnInit, Output, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { ProfileService } from '../../services/profile.service';
import { CloudImagePipe } from '../../shared/cloud-image.pipe';
import { IconComponent } from '../icon/icon.component';

// round icon in both navbars: Show User Profile -> /profile, Log Out -> asks right in the list, then tells the navbar
// (no popup here: the dialog code stays out of the always-loaded shell)
@Component({
  selector: 'app-profilemenu',
  standalone: true,
  imports: [CommonModule, IconComponent, CloudImagePipe],
  templateUrl: './profilemenu.component.html'
})
export class ProfilemenuComponent implements OnInit, OnDestroy {

  @Output() logoutClicked = new EventEmitter<void>();

  username: string = '';
  role: string = '';
  initial: string = '?';    // shown in the icon when there is no picture
  picture: string | null = null;
  menuOpen: boolean = false;
  askingLogout: boolean = false;

  // the question box, focused when it opens so Tab goes on to its buttons
  @ViewChild('logoutBox') logoutBox?: ElementRef<HTMLElement>;

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

  toggleMenu(): void {
    this.menuOpen = !this.menuOpen;
    this.askingLogout = false;
  }

  showProfile(): void {
    this.menuOpen = false;
    this.router.navigate(['/profile']);
  }

  // "Log Out": ask first. no button gets the focus, so Enter can't log out by accident
  askLogout(): void {
    this.askingLogout = true;
    setTimeout(() => this.logoutBox?.nativeElement.focus());
  }

  logOut(): void {
    this.closeMenu();
    this.logoutClicked.emit();
  }

  // Cancel, a click next to the list, or Escape while the question is shown
  closeMenu(): void {
    this.menuOpen = false;
    this.askingLogout = false;
  }

  @HostListener('document:keydown.escape')
  escape(): void {
    if (this.askingLogout) {
      this.closeMenu();
    }
  }
}
