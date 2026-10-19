import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FieldOption } from '../../shared/utils/field-option';
import { byIndex } from '../../shared/utils/track-by';

// one drop-down above a list, "All" is always its first line
// use: <app-filterbox label="Status" [options]="statusOptions" [(value)]="statusFilter" (valueChange)="page = 1"></app-filterbox>
@Component({
  selector: 'app-filterbox',
  templateUrl: './filter-box.component.html'
})
export class FilterBoxComponent {

  @Input() label: string = 'Filter';
  @Input() options: FieldOption[] = [];
  @Input() value: string = 'All';
  @Output() valueChange = new EventEmitter<string>();

  readonly byIndex = byIndex;
}
