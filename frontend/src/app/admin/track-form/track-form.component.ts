import { Component, Input } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FieldOption, toOptions } from '../../shared/utils/field-option';
import { TRACK_CATEGORIES } from '../../core/constants';
import { TRACK_PICTURES_MAX } from '../../shared/utils/validation';

// the boxes of a track + a preview of the card drivers will see. Add Track and Edit Track both use it
// use: <app-trackform [form]="trackForm"> the page's buttons </app-trackform>
@Component({
  selector: 'app-trackform',
  templateUrl: './track-form.component.html'
})
export class TrackFormComponent {

  @Input() form!: FormGroup;

  maxPictures: number = TRACK_PICTURES_MAX;
  categoryOptions: FieldOption[] = toOptions(TRACK_CATEGORIES);
  indoorOptions: FieldOption[] = [{ value: true, text: 'Indoor' }, { value: false, text: 'Outdoor' }];

  // the picture box is not a form control, so its list is put into the form by hand
  setPictures(urls: string[]): void {
    const pictures = this.form.get('imageUrls');
    pictures?.setValue(urls);
    pictures?.markAsTouched();
  }
}
