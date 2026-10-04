import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule } from '@angular/material/dialog';
import { LogoComponent } from '../components/logo/logo.component';
import { LoaderComponent } from '../components/loader/loader.component';
import { IconComponent } from '../components/icon/icon.component';
import { CloudImagePipe } from './cloud-image.pipe';
import { PopupDirective } from './popup.directive';

// what the always-loaded shell (navbars, chatbot, page loader) needs: logo, loading car, icons, picture pipe, popups
// popups are the one place where Angular Material is used (MatDialog, opened by the appPopup directive)
@NgModule({
  declarations: [LogoComponent, LoaderComponent, IconComponent, CloudImagePipe, PopupDirective],
  imports: [CommonModule, MatDialogModule],
  exports: [LogoComponent, LoaderComponent, IconComponent, CloudImagePipe, PopupDirective]
})
export class BasicModule { }
