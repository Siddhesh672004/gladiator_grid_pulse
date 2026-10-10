import { CommonModule } from '@angular/common';
import { Component, HostListener } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { BasicModule } from '../../shared/basic.module';
import { ProfilemenuComponent } from '../profilemenu/profilemenu.component';

// driver navbar. "Log Out" in the profile icon menu opens the logout popup of this navbar
@Component({
  selector: 'app-usernav',
  standalone: true,
  imports: [CommonModule, RouterModule, BasicModule, ProfilemenuComponent],
  templateUrl: './usernav.component.html'
})
export class UsernavComponent {

  menuOpen: boolean = false;       // the phone menu
  openMenu: string = '';           // the group whose list is open: 'tracks', ... or '' for none
  showLogoutPopup: boolean = false;

  constructor(private authService: AuthService, private router: Router) { }

  // "Tracks" / "Feedback" look active while one of their pages is open
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

  // "Log Out" in the profile menu: ask first
  askLogout(): void {
    this.showLogoutPopup = true;
    this.closeMenus();
  }

  logout(): void {
    this.showLogoutPopup = false;
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
