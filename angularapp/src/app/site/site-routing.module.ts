import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard } from '../components/authguard/auth.guard';
import { LandingComponent } from '../components/landing/landing.component';
import { HowitworksComponent } from '../components/howitworks/howitworks.component';
import { LoginComponent } from '../components/login/login.component';
import { AdminloginComponent } from '../components/adminlogin/adminlogin.component';
import { ForgotpasswordComponent } from '../components/forgotpassword/forgotpassword.component';
import { SetpasswordComponent } from '../components/setpassword/setpassword.component';
import { SignupComponent } from '../components/signup/signup.component';
import { HomePageComponent } from '../components/home-page/home-page.component';
import { UserprofileComponent } from '../components/userprofile/userprofile.component';
import { ErrorComponent } from '../components/error/error.component';

// every page whose url has no /admin or /user in front. home + profile need a login (AuthGuard)
const routes: Routes = [
  { path: '', component: LandingComponent, pathMatch: 'full' },
  { path: 'how-it-works', component: HowitworksComponent },
  { path: 'login', component: LoginComponent },
  { path: 'admin-login', component: AdminloginComponent },
  { path: 'forgot-password', component: ForgotpasswordComponent },
  // the link in a new admin's invite email
  { path: 'set-password', component: SetpasswordComponent },
  { path: 'signup', component: SignupComponent },
  { path: 'home', component: HomePageComponent, canActivate: [AuthGuard] },
  { path: 'profile', component: UserprofileComponent, canActivate: [AuthGuard] },
  { path: 'error', component: ErrorComponent },
  { path: '**', redirectTo: '/error' }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class SiteRoutingModule { }
