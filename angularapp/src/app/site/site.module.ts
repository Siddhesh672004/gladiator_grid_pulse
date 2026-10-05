import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { SiteRoutingModule } from './site-routing.module';
import { SharedModule } from '../shared/shared.module';
import { LandingComponent } from '../components/landing/landing.component';
import { HowitworksComponent } from '../components/howitworks/howitworks.component';
import { SiteheadComponent } from '../components/sitehead/sitehead.component';
import { SitefootComponent } from '../components/sitefoot/sitefoot.component';
import { AuthshellComponent } from '../components/authshell/authshell.component';
import { LoginComponent } from '../components/login/login.component';
import { AdminloginComponent } from '../components/adminlogin/adminlogin.component';
import { ForgotpasswordComponent } from '../components/forgotpassword/forgotpassword.component';
import { SignupComponent } from '../components/signup/signup.component';
import { HomePageComponent } from '../components/home-page/home-page.component';
import { UserprofileComponent } from '../components/userprofile/userprofile.component';
import { ErrorComponent } from '../components/error/error.component';

// lazy loaded: start page, how it works, the login pages, driver home, profile, error page
@NgModule({
  declarations: [
    LandingComponent,
    HowitworksComponent,
    SiteheadComponent,
    SitefootComponent,
    AuthshellComponent,
    LoginComponent,
    AdminloginComponent,
    ForgotpasswordComponent,
    SignupComponent,
    HomePageComponent,
    UserprofileComponent,
    ErrorComponent
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    SharedModule,
    SiteRoutingModule
  ]
})
export class SiteModule { }
