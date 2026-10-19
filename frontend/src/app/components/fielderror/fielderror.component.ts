import { Component, Input } from '@angular/core';
import { AbstractControl } from '@angular/forms';
import { errorText } from '../../validation';

// the red message under something that is not an app-field (pictures, stars, a tick box)
// same as app-field: the first broken rule only, once the control was touched or changed (errorText in validation.ts)
@Component({
  selector: 'app-fielderror',
  templateUrl: './fielderror.component.html'
})
export class FielderrorComponent {

  @Input() control: AbstractControl | null = null;
  @Input() label: string = '';
  @Input() requiredText: string = '';

  get message(): string {
    return errorText(this.control, this.label, this.requiredText);
  }
}
