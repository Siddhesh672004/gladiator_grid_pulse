import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

// shown while a list or a page is loading: the car drives along the track
@Component({
  selector: 'app-loader',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './loader.component.html'
})
export class LoaderComponent {

  @Input() label: string = 'Loading';
  @Input() brand: boolean = false;      // true = the GridPulse name above the track (full page loader)
}
