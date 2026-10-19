import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { UserviewtracksComponent } from '../components/userviewtracks/userviewtracks.component';
import { UserregisterraceComponent } from '../components/userregisterrace/userregisterrace.component';
import { UserviewmyregistrationsComponent } from '../components/userviewmyregistrations/userviewmyregistrations.component';
import { UseraddfeedbackComponent } from '../components/useraddfeedback/useraddfeedback.component';
import { UserviewfeedbackComponent } from '../components/userviewfeedback/userviewfeedback.component';
import { UserlivetimingComponent } from '../components/userlivetiming/userlivetiming.component';

// /user/** (the guard and the User role check are on the parent route in app.routes.ts)
// My Stats has its own download (chart.js)
const routes: Routes = [
  { path: 'view/tracks', component: UserviewtracksComponent },
  { path: 'register/:trackId', component: UserregisterraceComponent },
  { path: 'view/registrations', component: UserviewmyregistrationsComponent },
  { path: 'add/feedback', component: UseraddfeedbackComponent },
  { path: 'view/feedback', component: UserviewfeedbackComponent },
  { path: 'live-timing', component: UserlivetimingComponent },
  { path: 'stats', loadComponent: () => import('../components/userstats/userstats.component').then((m) => m.UserstatsComponent) },
  { path: '**', redirectTo: '/error' }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class UserRoutingModule { }
