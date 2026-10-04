import { Directive, EventEmitter, Input, OnChanges, OnDestroy, Output, TemplateRef } from '@angular/core';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { DETAILS_POPUP, POPUP } from './popup';

// every popup is an Angular Material dialog. its html stays in the page: <ng-template [appPopup]="trackToDelete">
// the popup is open while that value is set (true, or the row it is about) and closes when the page clears it
@Directive({
  selector: '[appPopup]'
})
export class PopupDirective implements OnChanges, OnDestroy {

  @Input() appPopup: any;
  @Input() closeOutside: boolean = false;          // details popups: a click outside closes them too
  @Output() closed = new EventEmitter<void>();     // the popup closed itself (click outside), the page clears its value

  private dialogRef?: MatDialogRef<any>;

  constructor(private template: TemplateRef<any>, private dialog: MatDialog) { }

  // runs whenever the value changes
  ngOnChanges(): void {
    if (this.appPopup && !this.dialogRef) {
      this.open();
    }
    if (!this.appPopup && this.dialogRef) {
      this.dialogRef.close();
    }
  }

  // the page was left while its popup was open
  ngOnDestroy(): void {
    this.dialogRef?.close();
  }

  private open(): void {
    if (this.closeOutside) {
      this.dialogRef = this.dialog.open(this.template, DETAILS_POPUP);
    } else {
      this.dialogRef = this.dialog.open(this.template, POPUP);
    }
    this.dialogRef.afterClosed().subscribe(() => {
      this.dialogRef = undefined;
      this.closed.emit();
    });
  }
}
