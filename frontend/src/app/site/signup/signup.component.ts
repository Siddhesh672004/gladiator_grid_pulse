import { Component } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Router } from '@angular/router';
import { SignupRequest } from '../../shared/models/user.model';
import { AuthService } from '../../core/services/auth.service';
import { DOB_EARLIEST, MIN_AGE, birthDateRules, codeRules, emailRules, licenseRules, mobileRules, newPasswordRules,
  recheckWhenChanged, required, sameAs, usernameRules, yearsAgoIso } from '../../shared/utils/validation';

// sign up in two steps: details -> emailed code (no account yet) -> code -> account -> login page (AuthService)
@Component({
  selector: 'app-signup',
  templateUrl: './signup.component.html'
})
export class SignupComponent {

  signupForm: FormGroup;
  codeForm: FormGroup;
  step: 'form' | 'code' = 'form';
  email: string = '';
  infoMessage: string = '';
  errorMessage: string = '';
  showModal: boolean = false;
  isSaving: boolean = false;
  isResending: boolean = false;
  earliestBirthDate: string = DOB_EARLIEST;
  latestBirthDate: string = yearsAgoIso(MIN_AGE);

  constructor(private fb: FormBuilder, private authService: AuthService, private router: Router) {
    this.signupForm = this.fb.group({
      username: ['', usernameRules], 
      email: ['', emailRules],
      mobileNumber: ['', mobileRules],
      dateOfBirth: ['', birthDateRules],
      driverLicenseNumber: ['', licenseRules],
      password: ['', newPasswordRules],
      confirmPassword: ['', [required, sameAs('password', 'Passwords do not match')]]
    });
    recheckWhenChanged(this.signupForm, 'password', 'confirmPassword');
    this.codeForm = this.fb.group({
      otp: ['', codeRules]
    });
    // a message from the server ("Email is already registered", "Wrong code") goes away when the driver types again
    this.signupForm.valueChanges.subscribe(() => this.errorMessage = '');
    this.codeForm.valueChanges.subscribe(() => this.errorMessage = '');
  }

  private details(): SignupRequest {
    const value = this.signupForm.value;
    return {
      username: value.username.trim(),
      email: value.email.trim(),
      mobileNumber: value.mobileNumber,
      dateOfBirth: value.dateOfBirth,
      driverLicenseNumber: value.driverLicenseNumber.trim(),
      password: value.password
    };
  }

  sendCode(): void {
    this.errorMessage = '';
    if (this.signupForm.invalid) {
      this.signupForm.markAllAsTouched();
      return;
    }
    this.isSaving = true;
    this.authService.sendSignupCode(this.details()).subscribe({
      next: (res) => {
        this.isSaving = false;
        this.email = res.email;
        this.infoMessage = res.message;
        this.step = 'code';
      },
      error: (err) => {
        this.isSaving = false;
        this.errorMessage = this.serverText(err);
      }
    });
  }

  register(): void {
    this.errorMessage = '';
    if (this.codeForm.invalid) {
      this.codeForm.markAllAsTouched();
      return;
    }
    // the same details as in step 1, plus the code from the email
    const user: SignupRequest = this.details();
    user.otp = this.codeForm.value.otp.trim();
    this.isSaving = true;
    this.authService.register(user).subscribe({
      next: () => {
        this.isSaving = false;
        this.showModal = true;
      },
      error: (err) => {
        this.isSaving = false;
        this.errorMessage = this.serverText(err);
      }
    });
  }

  // same call as step 1, the backend sends at most one code a minute
  resend(): void {
    // a double click must not ask for two emails
    if (this.isResending) {
      return;
    }
    this.errorMessage = '';
    this.infoMessage = '';
    this.isResending = true;
    this.authService.sendSignupCode(this.details()).subscribe({
      next: (res) => {
        this.isResending = false;
        this.infoMessage = res.message;
      },
      error: (err) => {
        this.isResending = false;
        this.errorMessage = this.serverText(err);
      }
    });
  }

  backToForm(): void {
    this.step = 'form';
    this.codeForm.reset();
    this.errorMessage = '';
    this.infoMessage = '';
  }

  closeModal(): void {
    this.showModal = false;
    this.router.navigate(['/login']);
  }

  private serverText(err: any): string {
    return err.error?.message || 'Registration failed. Please try again.';
  }
}
