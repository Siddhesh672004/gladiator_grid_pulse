import { Component } from '@angular/core';
import { PHOTO_CREDITS } from '../photos';
import { byIndex } from '../../shared/utils/track-by';
import { scrollBehavior } from '../../shared/utils/motion';

// bottom of the public pages: links, contact, who took the pictures, back to top
@Component({
  selector: 'app-sitefoot',
  templateUrl: './site-foot.component.html'
})
export class SiteFootComponent {

  credits = PHOTO_CREDITS;

  readonly byIndex = byIndex;

  toTop(): void {
    window.scrollTo({ top: 0, behavior: scrollBehavior() });
  }
}
