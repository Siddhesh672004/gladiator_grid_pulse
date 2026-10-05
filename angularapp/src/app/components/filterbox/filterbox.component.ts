import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FieldOption } from '../field/field.component';

// one drop-down above a list, "All" is always its first line
// use: <app-filterbox label="Status" [options]="statusOptions" [(value)]="statusFilter" (valueChange)="page = 1"></app-filterbox>
@Component({
  selector: 'app-filterbox',
  templateUrl: './filterbox.component.html'
})
export class FilterboxComponent {

  @Input() label: string = 'Filter';
  @Input() options: FieldOption[] = [];
  @Input() value: string = 'All';
  @Output() valueChange = new EventEmitter<string>();
}
