import { Component } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Router } from '@angular/router';
import { Login } from '../../shared/models/login.model';
import { AuthService } from '../../core/services/auth.service';
import { emailRules, required } from '../../shared/utils/validation';

// admin + super admin login (/admin-login): email + password -> AuthService.adminLogin, no code step
// a driver here gets "Invalid Email or Password", a disabled admin gets the backend message
@Component({
  selector: 'app-adminlogin',
  templateUrl: './admin-login.component.html'
})
export class AdminLoginComponent {

  loginForm: FormGroup;
  errorMessage: string = '';
  isSaving: boolean = false;

  constructor(private fb: FormBuilder, private authService: AuthService, private router: Router) {
    this.loginForm = this.fb.group({
      email: ['', emailRules],
      // only "required": an admin with an old short password must still log in
      password: ['', [required]]
    });
    // the "wrong login" line goes away when the admin types again
    this.loginForm.valueChanges.subscribe(() => this.errorMessage = '');
  }

  login(): void {
    this.errorMessage = '';
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }
    const login: Login = { email: this.loginForm.value.email.trim(), password: this.loginForm.value.password };
    this.isSaving = true;
    this.authService.adminLogin(login).subscribe({
      next: () => {
        this.isSaving = false;
        this.router.navigate(['/admin/dashboard']);
      },
      error: (err) => {
        this.isSaving = false;
        this.errorMessage = err.error?.message || 'Unable to reach the server. Please try again later.';
      }
    });
  }
}
