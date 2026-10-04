import { MatDialogConfig } from '@angular/material/dialog';

// how every popup of the app opens (Angular Material dialog):  this.dialog.open(this.deletePopup, POPUP)
// only the buttons of the popup close it, no button is focused at the start (Enter must not delete by accident)
export const POPUP: MatDialogConfig = {
  disableClose: true,
  autoFocus: false,
  maxWidth: 'calc(100vw - 32px)',
  panelClass: 'gp-popup',
  backdropClass: 'gp-popup-backdrop'
};

// a details popup (Show More): a click outside closes it too, the page behind is a little darker
export const DETAILS_POPUP: MatDialogConfig = {
  disableClose: false,
  autoFocus: false,
  maxWidth: 'calc(100vw - 32px)',
  panelClass: 'gp-popup',
  backdropClass: 'gp-popup-backdrop-dim'
};
