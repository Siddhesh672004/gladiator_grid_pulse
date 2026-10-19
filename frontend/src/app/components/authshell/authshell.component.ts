import { Component, Input } from '@angular/core';
import { PHOTOS, PHOTO_HEIGHTS, PHOTO_WIDTH } from '../../shared/photos';
import { byIndex } from '../../shared/track-by';

// the split screen of the four login pages: picture + a few words on the left, the page's own form on the right
// use: <app-authshell title="..." text="..." [points]="[...]" photo="race"> <form>...</form> </app-authshell>
@Component({
  selector: 'app-authshell',
  templateUrl: './authshell.component.html'
})
export class AuthshellComponent {

  @Input() title: string = '';
  @Input() text: string = '';
  @Input() points: string[] = [];
  @Input() photo: string = 'race';     // a name from shared/photos.ts

  readonly byIndex = byIndex;
  readonly photoWidth = PHOTO_WIDTH;

  get photoLink(): string {
    return PHOTOS[this.photo];
  }

  get photoHeight(): number {
    return PHOTO_HEIGHTS[this.photo];
  }
}
