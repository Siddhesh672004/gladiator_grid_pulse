import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { SiteRoutingModule } from './site-routing.module';
import { SharedModule } from '../shared/shared.module';
import { LandingComponent } from './landing/landing.component';
import { HowItWorksComponent } from './how-it-works/how-it-works.component';
import { SiteHeadComponent } from './site-head/site-head.component';
import { SiteFootComponent } from './site-foot/site-foot.component';
import { AuthShellComponent } from './auth-shell/auth-shell.component';
import { LoginComponent } from './login/login.component';
import { AdminLoginComponent } from './admin-login/admin-login.component';
import { ForgotPasswordComponent } from './forgot-password/forgot-password.component';
import { SetPasswordComponent } from './set-password/set-password.component';
import { SignupComponent } from './signup/signup.component';
import { HomePageComponent } from './account/home-page/home-page.component';
import { UserProfileComponent } from './account/profile/user-profile.component';
import { ErrorComponent } from './error/error.component';
import { RevealDirective } from './reveal.directive';

// lazy loaded: start page, how it works, the login pages, driver home, profile, error page
@NgModule({
  declarations: [
    LandingComponent,
    HowItWorksComponent,
    SiteHeadComponent,
    SiteFootComponent,
    AuthShellComponent,
    LoginComponent,
    AdminLoginComponent,
    ForgotPasswordComponent,
    SetPasswordComponent,
    SignupComponent,
    HomePageComponent,
    UserProfileComponent,
    ErrorComponent,
    RevealDirective
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    SharedModule,
    SiteRoutingModule
  ]
})
export class SiteModule { }
