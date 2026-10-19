import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule } from '@angular/material/dialog';
import { LogoComponent } from '../components/logo/logo.component';
import { LoaderComponent } from '../components/loader/loader.component';
import { IconComponent } from '../components/icon/icon.component';
import { CloudImagePipe } from './cloud-image.pipe';
import { PopupDirective } from './popup.directive';

// logo, loading car, icons, picture pipe and popups for the page modules (through SharedModule)
// popups are the one place where Angular Material is used (MatDialog, opened by the appPopup directive).
// the first four are standalone so the always-loaded shell can use them without the dialog code
@NgModule({
  declarations: [PopupDirective],
  imports: [CommonModule, MatDialogModule, LogoComponent, LoaderComponent, IconComponent, CloudImagePipe],
  exports: [LogoComponent, LoaderComponent, IconComponent, CloudImagePipe, PopupDirective]
})
export class BasicModule { }
