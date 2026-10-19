import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AdminAddTrackComponent } from './add-track/admin-add-track.component';
import { AdminEditTrackComponent } from './edit-track/admin-edit-track.component';
import { AdminViewTracksComponent } from './view-tracks/admin-view-tracks.component';
import { AdminAddSessionComponent } from './add-session/admin-add-session.component';
import { AdminViewSessionsComponent } from './view-sessions/admin-view-sessions.component';
import { AdminCompetitionControlComponent } from './competition-control/admin-competition-control.component';
import { AdminViewRegistrationsComponent } from './view-registrations/admin-view-registrations.component';
import { AdminViewFeedbackComponent } from './view-feedback/admin-view-feedback.component';
import { AdminRaceControlComponent } from './race-control/admin-race-control.component';
import { SuperAdminManageAdminsComponent } from './manage-admins/super-admin-manage-admins.component';
import { AdminViewPaymentsComponent } from './view-payments/admin-view-payments.component';
import { AdminManageUsersComponent } from './manage-users/admin-manage-users.component';
import { AdminCheckTicketComponent } from './check-ticket/admin-check-ticket.component';
import { ROLES } from '../core/constants';

// /admin/** (the guard and the Admin role check are on the parent route in app.routes.ts)
// manage-admins is for the SuperAdmin only. the dashboard has its own download (chart.js)
const routes: Routes = [
  {
    path: 'dashboard',
    loadComponent: () => import('./dashboard/admin-dashboard.component').then((m) => m.AdminDashboardComponent)
  },
  { path: 'manage-users', component: AdminManageUsersComponent },
  { path: 'add/track', component: AdminAddTrackComponent },
  { path: 'view/tracks', component: AdminViewTracksComponent },
  { path: 'edit/track/:id', component: AdminEditTrackComponent },
  { path: 'add/session', component: AdminAddSessionComponent },
  { path: 'edit/session/:id', component: AdminAddSessionComponent },
  { path: 'view/sessions', component: AdminViewSessionsComponent },
  { path: 'competition/:id', component: AdminCompetitionControlComponent },
  { path: 'view/registrations', component: AdminViewRegistrationsComponent },
  { path: 'check-ticket', component: AdminCheckTicketComponent },
  { path: 'view/feedback', component: AdminViewFeedbackComponent },
  { path: 'view/payments', component: AdminViewPaymentsComponent },
  { path: 'race-control', component: AdminRaceControlComponent },
  { path: 'manage-admins', component: SuperAdminManageAdminsComponent, data: { roles: [ROLES.SUPER_ADMIN] } },
  { path: '**', redirectTo: '/error' }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class AdminRoutingModule { }
