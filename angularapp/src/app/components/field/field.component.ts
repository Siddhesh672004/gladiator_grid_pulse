import { Component, Input } from '@angular/core';
import { AbstractControl, FormControl } from '@angular/forms';
import { errorText } from '../../validation';
import { byIndex } from '../../shared/track-by';

// one line of a drop-down box
export interface FieldOption {
  value: any;
  text: string;
}

// ['Circuit', 'Rally'] -> drop-down lines that show the value itself
export function toOptions(values: string[]): FieldOption[] {
  const options: FieldOption[] = [];
  for (const value of values) {
    options.push({ value: value, text: value });
  }
  return options;
}

// one form box: label, the box, a hint, and the first broken rule under it (errorText in validation.ts)
// a text box by default, [rows]="4" makes it a big text box, [options] makes it a drop-down
@Component({
  selector: 'app-field',
  templateUrl: './field.component.html'
})
export class FieldComponent {

  @Input() control: AbstractControl | null = null;
  @Input() label: string = '';
  @Input() name: string = '';            // the word in the error message, when it is not the label
  @Input() requiredText: string = '';    // own "is required" sentence, when the usual one reads wrong
  @Input() type: string = 'text';
  @Input() inputId: string = '';
  @Input() hint: string = '';
  @Input() autocomplete: string = 'off';
  @Input() min: string = '';             // first / last day of a date box
  @Input() max: string = '';
  @Input() rows: number = 0;
  @Input() options: FieldOption[] | null = null;
  @Input() optional: boolean = false;
  @Input() maxlength: number = 0;        // the browser stops typing at this length, 0 = no limit
  @Input() digits: boolean = false;      // only digits can be typed or pasted, phones show the number keys
  @Input() upper: boolean = false;       // typed letters become capitals

  showPassword: boolean = false;         // a password box gets the eye button on its own

  readonly byIndex = byIndex;

  // the template binds the box with [formControl], which wants a FormControl
  get box(): FormControl {
    return this.control as FormControl;
  }

  // the eye button turns a password box into a text box and back
  get boxType(): string {
    if (this.type === 'password' && this.showPassword) {
      return 'text';
    }
    return this.type;
  }

  get message(): string {
    return errorText(this.control, this.name || this.label, this.requiredText);
  }

  // shows while typing, after leaving the box, or after Submit (the page then marks every box as touched)
  get showError(): boolean {
    return this.message !== '';
  }

  // runs on every key and on paste: drops what this box does not take
  cleanTyping(event: Event): void {
    const typed = (event.target as HTMLInputElement).value;
    let text = typed;
    if (this.digits) {
      text = text.replace(/[^0-9]/g, '');
    }
    if (this.upper) {
      text = text.toUpperCase();
    }
    if (text !== typed) {
      this.box.setValue(text);
    }
  }

  // a digits box cleans a pasted text itself, before the browser cuts it to maxlength
  // too many digits = a country code in front: "+91 98765 43210" -> 9876543210
  pasteDigits(event: ClipboardEvent): void {
    if (!this.digits) {
      return;
    }
    event.preventDefault();
    const input = event.target as HTMLInputElement;
    const before = input.value.substring(0, input.selectionStart ?? input.value.length);
    const after = input.value.substring(input.selectionEnd ?? input.value.length);
    let pasted = (event.clipboardData?.getData('text') ?? '').replace(/[^0-9]/g, '');
    if (this.maxlength) {
      const room = Math.max(0, this.maxlength - before.length - after.length);
      pasted = pasted.substring(Math.max(0, pasted.length - room));
    }
    this.box.setValue(before + pasted + after);
    this.box.markAsDirty();
  }

  // a drop-down with nothing picked yet shows a "Select" line
  get nothingChosen(): boolean {
    return this.box.value === null || this.box.value === '';
  }
}
