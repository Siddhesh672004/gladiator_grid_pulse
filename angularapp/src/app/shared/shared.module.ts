import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { BasicModule } from './basic.module';
import { FielderrorComponent } from '../components/fielderror/fielderror.component';
import { FieldComponent } from '../components/field/field.component';
import { SearchboxComponent } from '../components/searchbox/searchbox.component';
import { FilterboxComponent } from '../components/filterbox/filterbox.component';
import { PaginatorComponent } from '../components/paginator/paginator.component';
import { ImagepickerComponent } from '../components/imagepicker/imagepicker.component';
import { CarouselComponent } from '../components/carousel/carousel.component';
import { PageheadComponent } from '../components/pagehead/pagehead.component';
import { FlagComponent } from '../components/flag/flag.component';
import { StartlightsComponent } from '../components/startlights/startlights.component';
import { EmptyComponent } from '../components/empty/empty.component';
import { RevealDirective } from './reveal.directive';

const PIECES = [
  FielderrorComponent, FieldComponent, SearchboxComponent, FilterboxComponent, PaginatorComponent, ImagepickerComponent, CarouselComponent,
  PageheadComponent, FlagComponent, StartlightsComponent, EmptyComponent, RevealDirective
];

// used by every page module (site, admin, user): the reusable pieces + BasicModule
@NgModule({
  declarations: PIECES,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, BasicModule],
  exports: [...PIECES, BasicModule]
})
export class SharedModule { }
