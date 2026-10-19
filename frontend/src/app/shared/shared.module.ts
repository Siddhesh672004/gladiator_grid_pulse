import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { BasicModule } from './basic.module';
import { FieldErrorComponent } from './ui/field-error/field-error.component';
import { FieldComponent } from './ui/field/field.component';
import { SearchBoxComponent } from './ui/search-box/search-box.component';
import { PaginatorComponent } from './ui/paginator/paginator.component';
import { ImagePickerComponent } from './ui/image-picker/image-picker.component';
import { PageHeadComponent } from './ui/page-head/page-head.component';
import { FlagComponent } from './ui/flag/flag.component';
import { StartLightsComponent } from './ui/start-lights/start-lights.component';
import { EmptyComponent } from './ui/empty/empty.component';
import { SessionLabelPipe } from './ui/session-label.pipe';
import { SessionTimePipe } from './ui/session-time.pipe';

const PIECES = [
  FieldErrorComponent, FieldComponent, SearchBoxComponent, PaginatorComponent, ImagePickerComponent,
  PageHeadComponent, FlagComponent, StartLightsComponent, EmptyComponent
];
const PIPES = [SessionLabelPipe, SessionTimePipe];

// used by every page module (site, admin, user): the reusable pieces, the session pipes + BasicModule
@NgModule({
  declarations: PIECES,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, BasicModule, ...PIPES],
  exports: [...PIECES, ...PIPES, BasicModule]
})
export class SharedModule { }
