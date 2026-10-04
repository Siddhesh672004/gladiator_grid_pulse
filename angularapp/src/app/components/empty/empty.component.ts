import { Component, Input } from '@angular/core';

// shown when a list has no rows: an empty grid slot, one line, and the next step as a button
// use: <app-empty title="No bookings yet" text="..."> <a class="btn">Book a session</a> </app-empty>
@Component({
  selector: 'app-empty',
  templateUrl: './empty.component.html'
})
export class EmptyComponent {

  @Input() title: string = '';
  @Input() text: string = '';
}
