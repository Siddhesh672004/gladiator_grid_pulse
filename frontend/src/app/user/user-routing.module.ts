import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { UserViewTracksComponent } from './view-tracks/user-view-tracks.component';
import { UserRegisterRaceComponent } from './register-race/user-register-race.component';
import { UserViewMyRegistrationsComponent } from './view-my-registrations/user-view-my-registrations.component';
import { UserAddFeedbackComponent } from './add-feedback/user-add-feedback.component';
import { UserViewFeedbackComponent } from './view-feedback/user-view-feedback.component';
import { UserLiveTimingComponent } from './live-timing/user-live-timing.component';

// /user/** (the guard and the User role check are on the parent route in app.routes.ts)
// My Stats has its own download (chart.js)
const routes: Routes = [
  { path: 'view/tracks', component: UserViewTracksComponent },
  { path: 'register/:trackId', component: UserRegisterRaceComponent },
  { path: 'view/registrations', component: UserViewMyRegistrationsComponent },
  { path: 'add/feedback', component: UserAddFeedbackComponent },
  { path: 'view/feedback', component: UserViewFeedbackComponent },
  { path: 'live-timing', component: UserLiveTimingComponent },
  { path: 'stats', loadComponent: () => import('./stats/user-stats.component').then((m) => m.UserStatsComponent) },
  { path: '**', redirectTo: '/error' }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class UserRoutingModule { }
