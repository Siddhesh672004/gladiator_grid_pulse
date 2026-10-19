import { AbstractControl, FormGroup, ValidationErrors, ValidatorFn } from '@angular/forms';

// form rules of the whole app, twin of core-service platform/validation/ValidationRules.java (keep the two the same)
// a rule returns null when the value is fine, or an object that says what is wrong

export const EMAIL_MAX = 100;
export const EMAIL_NO_SPACE_PATTERN = /^\S*$/;
export const EMAIL_ONE_AT_PATTERN = /^[^@]*@[^@]*$/;
export const EMAIL_DOMAIN_PATTERN = /^.*@.+\..+$/;
export const EMAIL_PATTERN = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
export const DIGITS_PATTERN = /^[0-9]*$/;
export const MOBILE_LENGTH = 10;
export const MOBILE_LENGTH_PATTERN = /^.{10}$/;
export const MOBILE_START_PATTERN = /^[6-9].*$/;
export const CODE_LENGTH = 6;
export const CODE_LENGTH_PATTERN = /^.{6}$/;
export const USERNAME_PATTERN = /^[A-Za-z ]+$/;
export const USERNAME_ONE_SPACE_PATTERN = /^[^ ]+( [^ ]+)*$/;
export const USERNAME_MIN = 3;
export const USERNAME_MAX = 50;
export const PASSWORD_MIN = 8;
export const PASSWORD_MAX = 64;
export const LICENSE_LENGTH = 6;
export const LICENSE_START_PATTERN = /^DL.*$/i;
export const LICENSE_DIGITS_PATTERN = /^DL[0-9]{4}$/i;
export const HAS_LETTER_PATTERN = /^.*[A-Za-z].*$/;
export const PRICE_MAX = 100000;
export const MIN_AGE = 18;
export const DOB_EARLIEST = '1920-01-01';
export const PICTURE_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
export const PICTURE_MAX_MB = 5;
export const TRACK_PICTURES_MAX = 5;

// the text of a box without the spaces around it (the backend trims the same way)
function textOf(control: AbstractControl): string {
  const value = control.value;
  return value === null || value === undefined ? '' : String(value).trim();
}

// empty or only spaces = not filled in. false and 0 are real values
export function required(control: AbstractControl): ValidationErrors | null {
  return textOf(control) === '' ? { required: true } : null;
}

// the message is the key, so two broken patterns of one box both stay and the first one is shown
// an empty box passes here, "required" is its own rule
export function matches(pattern: RegExp, message: string): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const text = textOf(control);
    if (text === '' || pattern.test(text)) {
      return null;
    }
    return { [message]: message };
  };
}

// length of the trimmed text, same error keys as Validators.minLength / maxLength
export function minChars(min: number): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const text = textOf(control);
    return text !== '' && text.length < min ? { minlength: { requiredLength: min } } : null;
  };
}

export function maxChars(max: number): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    return textOf(control).length > max ? { maxlength: { requiredLength: max } } : null;
  };
}

// same order as ValidationRules.passwordProblem in java. returns the first broken rule, '' = fine
// a password is never trimmed
export function passwordProblem(password: string): string {
  if (password.length < PASSWORD_MIN) {
    return `Password must be at least ${PASSWORD_MIN} characters`;
  }
  if (password.length > PASSWORD_MAX) {
    return `Password must be at most ${PASSWORD_MAX} characters`;
  }
  if (password.includes(' ')) {
    return 'Password cannot have spaces';
  }
  if (!/[A-Z]/.test(password)) {
    return 'Password must have at least one uppercase letter';
  }
  if (!/[a-z]/.test(password)) {
    return 'Password must have at least one lowercase letter';
  }
  if (!/[0-9]/.test(password)) {
    return 'Password must have at least one digit';
  }
  if (!/[^A-Za-z0-9 ]/.test(password)) {
    return 'Password must have at least one special character';
  }
  return '';
}

// new passwords only (sign up, reset, add admin), the login pages do not use it
export function strongPassword(control: AbstractControl): ValidationErrors | null {
  const problem = control.value ? passwordProblem(control.value) : '';
  return problem ? { password: problem } : null;
}

export function sameAs(otherField: string, message: string): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const other = control.parent?.get(otherField)?.value;
    return control.value && control.value !== other ? { sameAs: message } : null;
  };
}

// later than another time box ("10:00" > "08:30" works as text)
export function after(otherField: string, message: string): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const other = control.parent?.get(otherField)?.value;
    return control.value && other && control.value <= other ? { after: message } : null;
  };
}

// like after, but the same time is fine too
export function notBefore(otherField: string, message: string): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const other = control.parent?.get(otherField)?.value;
    return control.value && other && control.value < other ? { notBefore: message } : null;
  };
}

// a time box that must be at least some minutes after another time box
export function minMinutesAfter(otherField: string, minutes: number, message: string): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const other = control.parent?.get(otherField)?.value;
    if (!control.value || !other) {
      return null;
    }
    return minutesOf(control.value) < minutesOf(other) + minutes ? { minMinutesAfter: message } : null;
  };
}

// a time box: on today's date the time must not be over yet
export function notPassedToday(dateField: string, message: string): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const date = control.parent?.get(dateField)?.value;
    return control.value && date === todayIso() && control.value < nowTime() ? { notPassedToday: message } : null;
  };
}

export function notInPast(message: string): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    return control.value && control.value < todayIso() ? { notInPast: message } : null;
  };
}

// a date box: at most that many months from today. a year with 5 digits ("20260-01-01") is too far as well
export function withinMonths(months: number, message: string): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const date = control.value;
    return date && (date.length > 10 || date > monthsAheadIso(months)) ? { withinMonths: message } : null;
  };
}

// "2026-10-09T16:05" in India (always 5:30 ahead of UTC), whatever the clock of the browser says
// the backend checks dates and times in India time too
function indiaNow(): string {
  return new Date(Date.now() + 330 * 60 * 1000).toISOString().substring(0, 16);
}

export function todayIso(): string {
  return indiaNow().substring(0, 10);
}

// yearsAgoIso(18) = the latest birthday of an adult
export function yearsAgoIso(years: number): string {
  return addMonthsIso(todayIso(), -12 * years);
}

export function monthsAheadIso(months: number): string {
  return addMonthsIso(todayIso(), months);
}

// a date moved by whole months, Jan 31 + 1 month = Feb 28 (like plusMonths / plusYears in java)
function addMonthsIso(date: string, months: number): string {
  const [year, month, day] = date.split('-').map(Number);
  const firstOfMonth = new Date(year, month - 1 + months, 1);
  const lastDay = new Date(firstOfMonth.getFullYear(), firstOfMonth.getMonth() + 1, 0).getDate();
  return isoDate(firstOfMonth.getFullYear(), firstOfMonth.getMonth(), Math.min(day, lastDay));
}

// month counts from 0 like in Date
function isoDate(year: number, month: number, day: number): string {
  return `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
}

function nowTime(): string {
  return indiaNow().substring(11);
}

// "08:30" -> 510
function minutesOf(time: string): number {
  const [hours, minutes] = time.split(':');
  return Number(hours) * 60 + Number(minutes);
}

// in the past, not before 1920 and at least MIN_AGE years ago (@Past + @Adult in java)
export function adult(control: AbstractControl): ValidationErrors | null {
  const value = control.value;
  if (!value) {
    return null;
  }
  // a year with 5 digits is in the future too
  if (value.length > 10 || value >= todayIso()) {
    return { adult: 'Date of birth must be in the past' };
  }
  if (value < DOB_EARLIEST) {
    return { adult: 'Please enter a real date of birth' };
  }
  // the 18th birthday itself counts
  return addMonthsIso(value, 12 * MIN_AGE) > todayIso() ? { adult: `You must be at least ${MIN_AGE} years old` } : null;
}

// same order and texts as CloudinaryServiceImpl.checkPicture in java, '' = fine
export function pictureProblem(file: File): string {
  if (file.size === 0) {
    return 'Please choose a picture';
  }
  if (!PICTURE_TYPES.includes(file.type)) {
    return 'Only JPG, PNG or WebP pictures can be uploaded';
  }
  if (file.size > PICTURE_MAX_MB * 1024 * 1024) {
    return `A picture must be at most ${PICTURE_MAX_MB} MB`;
  }
  return '';
}

// sameAs / after look at a second box: when that one changes, check this one again
export function recheckWhenChanged(form: FormGroup, changedField: string, dependentField: string): void {
  form.get(changedField)?.valueChanges.subscribe(() => form.get(dependentField)?.updateValueAndValidity());
}

export const emailRules: ValidatorFn[] = [required, maxChars(EMAIL_MAX),
  matches(EMAIL_NO_SPACE_PATTERN, 'Email cannot have spaces'),
  matches(EMAIL_ONE_AT_PATTERN, 'Email must have exactly one @'),
  matches(EMAIL_DOMAIN_PATTERN, 'Email must have a domain after @, like gmail.com'),
  matches(EMAIL_PATTERN, 'Please enter a valid email')];
export const usernameRules: ValidatorFn[] = [required, minChars(USERNAME_MIN), maxChars(USERNAME_MAX),
  matches(USERNAME_PATTERN, 'Username can only have letters and spaces'),
  matches(USERNAME_ONE_SPACE_PATTERN, 'Username cannot have two spaces in a row')];
export const optionalMobileRules: ValidatorFn[] = [matches(DIGITS_PATTERN, 'Mobile number can only have digits'),
  matches(MOBILE_LENGTH_PATTERN, `Mobile number must be exactly ${MOBILE_LENGTH} digits`),
  matches(MOBILE_START_PATTERN, 'Mobile number must start with 6, 7, 8 or 9')];
export const mobileRules: ValidatorFn[] = [required, ...optionalMobileRules];
export const codeRules: ValidatorFn[] = [required, matches(DIGITS_PATTERN, 'Code can only have digits'),
  matches(CODE_LENGTH_PATTERN, `Code must be exactly ${CODE_LENGTH} digits`)];
export const newPasswordRules: ValidatorFn[] = [required, strongPassword, passwordFits];
export const birthDateRules: ValidatorFn[] = [required, adult];
export const licenseRules: ValidatorFn[] = [required, matches(LICENSE_START_PATTERN, 'License number must start with DL'),
  matches(LICENSE_DIGITS_PATTERN, 'License number must have exactly 4 digits after DL')];

// the first broken rule of one box as text, '' = nothing to show
// errors keep the order of the rule list, so the first key is the first broken rule
export function errorText(control: AbstractControl | null, label: string, requiredText: string = ''): string {
  if (!control || !control.errors || !(control.touched || control.dirty)) {
    return '';
  }
  const key = Object.keys(control.errors)[0];
  const error = control.errors[key];
  if (key === 'required') {
    return requiredText || `${label} is required`;
  }
  if (key === 'minlength') {
    return `${label} must be at least ${error.requiredLength} characters`;
  }
  if (key === 'maxlength') {
    return `${label} must be at most ${error.requiredLength} characters`;
  }
  if (key === 'min') {
    return `${label} must be at least ${error.min}`;
  }
  if (key === 'max') {
    return `${label} must be at most ${error.max}`;
  }
  // our own rules (matches, sameAs, ...): the value is the message
  return String(error);
}

// the server hashes a password with BCrypt, which takes 72 bytes at most (a letter outside English is 2 or 3 bytes)
// twin of the check in StrongPasswordValidator.java, goes after newPasswordRules
export const PASSWORD_MAX_BYTES = 72;
export function passwordFits(control: AbstractControl): ValidationErrors | null {
  const bytes = new TextEncoder().encode(control.value || '').length;
  return bytes > PASSWORD_MAX_BYTES ? { passwordFits: 'Password is too long, please use a shorter one' } : null;
}
