import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard } from '../core/guards/auth.guard';
import { LandingComponent } from './landing/landing.component';
import { HowItWorksComponent } from './how-it-works/how-it-works.component';
import { LoginComponent } from './login/login.component';
import { AdminLoginComponent } from './admin-login/admin-login.component';
import { ForgotPasswordComponent } from './forgot-password/forgot-password.component';
import { SetPasswordComponent } from './set-password/set-password.component';
import { SignupComponent } from './signup/signup.component';
import { HomePageComponent } from './account/home-page/home-page.component';
import { UserProfileComponent } from './account/profile/user-profile.component';
import { ErrorComponent } from './error/error.component';

// every page whose url has no /admin or /user in front. home + profile need a login (AuthGuard)
const routes: Routes = [
  { path: '', component: LandingComponent, pathMatch: 'full' },
  { path: 'how-it-works', component: HowItWorksComponent },
  { path: 'login', component: LoginComponent },
  { path: 'admin-login', component: AdminLoginComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  // the link in a new admin's invite email
  { path: 'set-password', component: SetPasswordComponent },
  { path: 'signup', component: SignupComponent },
  { path: 'home', component: HomePageComponent, canActivate: [AuthGuard] },
  { path: 'profile', component: UserProfileComponent, canActivate: [AuthGuard] },
  { path: 'error', component: ErrorComponent },
  { path: '**', redirectTo: '/error' }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class SiteRoutingModule { }
