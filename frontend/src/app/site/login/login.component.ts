import { Component } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Router } from '@angular/router';
import { Login } from '../../shared/models/login.model';
import { AuthService } from '../../core/services/auth.service';
import { emailRules, required } from '../../shared/utils/validation';
import { errorMessage } from '../../shared/utils/error-message';

// driver login: email + password -> AuthService.login -> /home. admins use /admin-login
@Component({
  selector: 'app-login',
  templateUrl: './login.component.html'
})
export class LoginComponent {

  loginForm: FormGroup;
  errorMessage: string = '';
  isSaving: boolean = false;

  constructor(private fb: FormBuilder, private authService: AuthService, private router: Router) {
    this.loginForm = this.fb.group({
      email: ['', emailRules],
      // only "required": the strong password rule is for new passwords, old accounts must still log in
      password: ['', [required]]
    });
    // the "wrong login" line goes away when the driver types again
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
    this.authService.login(login).subscribe({
      next: () => {
        this.isSaving = false;
        this.router.navigate(['/home']);
      },
      error: (err) => {
        this.isSaving = false;
        // a wrong login and a deactivated account are both 401, the server's text says which one it is
        this.errorMessage = errorMessage(err, 'Unable to reach the server. Please try again later.');
      }
    });
  }
}
