import { Component } from '@angular/core';
import { PHOTO_CREDITS } from '../../shared/photos';
import { byIndex } from '../../shared/track-by';

// bottom of the public pages: links, contact, who took the pictures, back to top
@Component({
  selector: 'app-sitefoot',
  templateUrl: './sitefoot.component.html'
})
export class SitefootComponent {

  credits = PHOTO_CREDITS;

  readonly byIndex = byIndex;

  toTop(): void {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
}
