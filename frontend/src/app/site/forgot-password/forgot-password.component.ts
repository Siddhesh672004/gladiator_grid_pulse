import { Component } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { codeRules, emailRules, newPasswordRules, recheckWhenChanged, required, sameAs } from '../../shared/utils/validation';
import { errorMessage } from '../../shared/utils/error-message';

// /forgot-password for drivers and admins: email -> reset code by mail -> code + new password -> login (AuthService)
@Component({
  selector: 'app-forgotpassword',
  templateUrl: './forgot-password.component.html'
})
export class ForgotPasswordComponent {

  emailForm: FormGroup;
  resetForm: FormGroup;
  step: 'email' | 'reset' | 'done' = 'email';
  email: string = '';
  infoMessage: string = '';
  errorMessage: string = '';
  isSaving: boolean = false;
  loginPage: string = '/login';      // "Back to login" goes to the page the visitor came from

  constructor(private fb: FormBuilder, private authService: AuthService, route: ActivatedRoute) {
    if (route.snapshot.queryParamMap.get('from') === 'admin') {
      this.loginPage = '/admin-login';
    }
    this.emailForm = this.fb.group({
      email: ['', emailRules]
    });
    this.resetForm = this.fb.group({
      code: ['', codeRules],
      newPassword: ['', newPasswordRules],
      confirmPassword: ['', [required, sameAs('newPassword', 'Passwords do not match')]]
    });
    recheckWhenChanged(this.resetForm, 'newPassword', 'confirmPassword');
    // a message from the server ("Wrong code. 4 tries left.") goes away when the visitor types again
    this.emailForm.valueChanges.subscribe(() => this.errorMessage = '');
    this.resetForm.valueChanges.subscribe(() => this.errorMessage = '');
  }

  sendCode(): void {
    this.errorMessage = '';
    if (this.emailForm.invalid) {
      this.emailForm.markAllAsTouched();
      return;
    }
    this.email = this.emailForm.value.email.trim();
    this.isSaving = true;
    this.authService.forgotPassword(this.email).subscribe({
      next: (res) => {
        this.isSaving = false;
        this.infoMessage = res.message;
        this.step = 'reset';
      },
      error: (err) => {
        this.isSaving = false;
        this.errorMessage = errorMessage(err, 'Unable to reach the server. Please try again later.');
      }
    });
  }

  reset(): void {
    this.errorMessage = '';
    if (this.resetForm.invalid) {
      this.resetForm.markAllAsTouched();
      return;
    }
    const value = this.resetForm.value;
    this.isSaving = true;
    this.authService.resetPassword(this.email, value.code.trim(), value.newPassword).subscribe({
      next: (res) => {
        this.isSaving = false;
        this.infoMessage = res.message;
        this.step = 'done';
      },
      error: (err) => {
        this.isSaving = false;
        this.errorMessage = errorMessage(err, 'Unable to reach the server. Please try again later.');
      }
    });
  }
}
