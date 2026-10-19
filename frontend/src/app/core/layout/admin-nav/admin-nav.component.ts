import { CommonModule } from '@angular/common';
import { Component, HostListener } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { IconComponent } from '../../../shared/ui/icon/icon.component';
import { LogoComponent } from '../../../shared/ui/logo/logo.component';
import { ProfileMenuComponent } from '../profile-menu/profile-menu.component';

// admin navbar (Admin + SuperAdmin) with the round profile icon (ProfileMenuComponent), which asks before Log Out
// only the SuperAdmin sees the "Admins" link
@Component({
  selector: 'app-adminnav',
  standalone: true,
  imports: [CommonModule, RouterModule, LogoComponent, IconComponent, ProfileMenuComponent],
  templateUrl: './admin-nav.component.html'
})
export class AdminNavComponent {

  isSuperAdmin: boolean;
  menuOpen: boolean = false;       // the phone menu
  openMenu: string = '';           // the group whose list is open: 'tracks', ... or '' for none

  constructor(private authService: AuthService, private router: Router) {
    this.isSuperAdmin = this.authService.isSuperAdmin();
  }

  // "Tracks" / "Sessions" look active while one of their pages is open
  isOn(part: string): boolean {
    return this.router.url.includes(part);
  }

  // a group button: open its list, or close it when it is open already
  toggleMenu(name: string): void {
    if (this.openMenu === name) {
      this.openMenu = '';
    } else {
      this.openMenu = name;
    }
  }

  // every link calls this; the Escape key too
  @HostListener('document:keydown.escape')
  closeMenus(): void {
    this.menuOpen = false;
    this.openMenu = '';
  }

  // "Yes, Logout" in the profile menu
  logout(): void {
    this.closeMenus();
    this.authService.logout();
    this.router.navigate(['/admin-login']);
  }
}
