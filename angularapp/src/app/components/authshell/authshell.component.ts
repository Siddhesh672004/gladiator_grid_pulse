import { Component, Input } from '@angular/core';
import { PHOTOS } from '../../shared/photos';

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

  get photoLink(): string {
    return PHOTOS[this.photo];
  }
}
