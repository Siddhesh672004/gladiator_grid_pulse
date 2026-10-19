import { CommonModule } from '@angular/common';
import { Component, HostListener } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { IconComponent } from '../icon/icon.component';
import { LogoComponent } from '../logo/logo.component';
import { ProfilemenuComponent } from '../profilemenu/profilemenu.component';

// driver navbar. "Log Out" in the profile icon menu asks there first, then this navbar logs out
@Component({
  selector: 'app-usernav',
  standalone: true,
  imports: [CommonModule, RouterModule, LogoComponent, IconComponent, ProfilemenuComponent],
  templateUrl: './usernav.component.html'
})
export class UsernavComponent {

  menuOpen: boolean = false;       // the phone menu
  openMenu: string = '';           // the group whose list is open: 'tracks', ... or '' for none

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

  // "Yes, Logout" in the profile menu
  logout(): void {
    this.closeMenus();
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
