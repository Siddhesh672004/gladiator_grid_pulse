import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AdminaddtrackComponent } from '../components/adminaddtrack/adminaddtrack.component';
import { AdmineditrackComponent } from '../components/admineditrack/admineditrack.component';
import { AdminviewtracksComponent } from '../components/adminviewtracks/adminviewtracks.component';
import { AdminaddsessionComponent } from '../components/adminaddsession/adminaddsession.component';
import { AdminviewsessionsComponent } from '../components/adminviewsessions/adminviewsessions.component';
import { AdmincompetitioncontrolComponent } from '../components/admincompetitioncontrol/admincompetitioncontrol.component';
import { AdminviewregistrationsComponent } from '../components/adminviewregistrations/adminviewregistrations.component';
import { AdminviewfeedbackComponent } from '../components/adminviewfeedback/adminviewfeedback.component';
import { AdminracecontrolComponent } from '../components/adminracecontrol/adminracecontrol.component';
import { SuperadminmanageadminsComponent } from '../components/superadminmanageadmins/superadminmanageadmins.component';
import { AdminviewpaymentsComponent } from '../components/adminviewpayments/adminviewpayments.component';
import { AdminmanageusersComponent } from '../components/adminmanageusers/adminmanageusers.component';
import { AdmincheckticketComponent } from '../components/admincheckticket/admincheckticket.component';
import { ROLES } from '../constants';

// /admin/** (the guard and the Admin role check are on the parent route in app.routes.ts)
// manage-admins is for the SuperAdmin only. the dashboard has its own download (chart.js)
const routes: Routes = [
  {
    path: 'dashboard',
    loadComponent: () => import('../components/admindashboard/admindashboard.component').then((m) => m.AdmindashboardComponent)
  },
  { path: 'manage-users', component: AdminmanageusersComponent },
  { path: 'add/track', component: AdminaddtrackComponent },
  { path: 'view/tracks', component: AdminviewtracksComponent },
  { path: 'edit/track/:id', component: AdmineditrackComponent },
  { path: 'add/session', component: AdminaddsessionComponent },
  { path: 'edit/session/:id', component: AdminaddsessionComponent },
  { path: 'view/sessions', component: AdminviewsessionsComponent },
  { path: 'competition/:id', component: AdmincompetitioncontrolComponent },
  { path: 'view/registrations', component: AdminviewregistrationsComponent },
  { path: 'check-ticket', component: AdmincheckticketComponent },
  { path: 'view/feedback', component: AdminviewfeedbackComponent },
  { path: 'view/payments', component: AdminviewpaymentsComponent },
  { path: 'race-control', component: AdminracecontrolComponent },
  { path: 'manage-admins', component: SuperadminmanageadminsComponent, data: { roles: [ROLES.SUPER_ADMIN] } },
  { path: '**', redirectTo: '/error' }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class AdminRoutingModule { }
