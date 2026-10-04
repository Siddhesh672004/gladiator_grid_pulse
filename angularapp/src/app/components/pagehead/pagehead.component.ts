import { Component, Input } from '@angular/core';

// the top of a page: title, one line under it, and the page's buttons on the right
// use: <app-pagehead title="Tracks" subtitle="..."> <button>...</button> </app-pagehead>
@Component({
  selector: 'app-pagehead',
  templateUrl: './pagehead.component.html'
})
export class PageheadComponent {

  @Input() title: string = '';
  @Input() subtitle: string = '';
}
