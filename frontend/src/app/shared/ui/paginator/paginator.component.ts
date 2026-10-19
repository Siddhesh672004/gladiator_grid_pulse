import { Component, EventEmitter, Input, Output } from '@angular/core';
import { pageCount } from '../../utils/paging';

// page buttons under a table, hidden when the list fits one page. the list page owns the page number
// use: <app-paginator [total]="rows.length" [page]="page" [pageSize]="pageSize" (pageChange)="page = $event"></app-paginator>
@Component({
  selector: 'app-paginator',
  templateUrl: './paginator.component.html'
})
export class PaginatorComponent {

  @Input() total: number = 0;
  @Input() page: number = 1;
  @Input() pageSize: number = 10;
  @Output() pageChange = new EventEmitter<number>();

  get lastPage(): number {
    return pageCount(this.total, this.pageSize);
  }

  // same rule as pageOf: a list that got shorter shows its last page
  get shownPage(): number {
    return Math.min(this.page, this.lastPage);
  }

  // "11 – 20 of 46": the first and the last row on this page
  get firstRow(): number {
    return (this.shownPage - 1) * this.pageSize + 1;
  }

  get lastRow(): number {
    return Math.min(this.shownPage * this.pageSize, this.total);
  }

  goTo(page: number): void {
    this.pageChange.emit(page);
  }
}
