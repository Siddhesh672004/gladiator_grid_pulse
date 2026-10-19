import { Component } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { newPasswordRules, recheckWhenChanged, required, sameAs } from '../../shared/utils/validation';

// /set-password?email=..&token=.. : the link in a new admin's invite email -> choose a password -> Admin Login
@Component({
  selector: 'app-setpassword',
  templateUrl: './set-password.component.html'
})
export class SetPasswordComponent {

  passwordForm: FormGroup;
  email: string;
  private token: string;
  done: boolean = false;
  infoMessage: string = '';
  errorMessage: string = '';
  isSaving: boolean = false;

  constructor(private fb: FormBuilder, private authService: AuthService, route: ActivatedRoute) {
    this.email = route.snapshot.queryParamMap.get('email') ?? '';
    this.token = route.snapshot.queryParamMap.get('token') ?? '';
    this.passwordForm = this.fb.group({
      newPassword: ['', newPasswordRules],
      confirmPassword: ['', [required, sameAs('newPassword', 'Passwords do not match')]]
    });
    recheckWhenChanged(this.passwordForm, 'newPassword', 'confirmPassword');
    // a message from the server goes away when the visitor types again
    this.passwordForm.valueChanges.subscribe(() => this.errorMessage = '');
  }

  // a link copied only half has no email or token
  get linkComplete(): boolean {
    return this.email !== '' && this.token !== '';
  }

  save(): void {
    this.errorMessage = '';
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }
    this.isSaving = true;
    this.authService.setInitialPassword(this.email, this.token, this.passwordForm.value.newPassword).subscribe({
      next: (res) => {
        this.isSaving = false;
        this.infoMessage = res.message;
        this.done = true;
      },
      error: (err) => {
        this.isSaving = false;
        this.errorMessage = err.error?.message || 'Unable to reach the server. Please try again later.';
      }
    });
  }
}
