import { Component, EventEmitter, Input, Output } from '@angular/core';

// the search box above a list
// use: <app-searchbox label="Search by track name" [(text)]="searchText" (textChange)="page = 1"></app-searchbox>
@Component({
  selector: 'app-searchbox',
  templateUrl: './searchbox.component.html'
})
export class SearchboxComponent {

  @Input() label: string = 'Search';
  @Input() text: string = '';
  @Output() textChange = new EventEmitter<string>();
}
