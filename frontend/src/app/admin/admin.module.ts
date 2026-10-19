import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { AdminRoutingModule } from './admin-routing.module';
import { SharedModule } from '../shared/shared.module';
import { AdminAddTrackComponent } from './add-track/admin-add-track.component';
import { TrackFormComponent } from './track-form/track-form.component';
import { FilterBoxComponent } from './filter-box/filter-box.component';
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

// lazy loaded admin pages (the dashboard is standalone, see the routing)
@NgModule({
  declarations: [
    AdminAddTrackComponent,
    TrackFormComponent,
    FilterBoxComponent,
    AdminEditTrackComponent,
    AdminViewTracksComponent,
    AdminAddSessionComponent,
    AdminViewSessionsComponent,
    AdminCompetitionControlComponent,
    AdminViewRegistrationsComponent,
    AdminViewFeedbackComponent,
    AdminRaceControlComponent,
    SuperAdminManageAdminsComponent,
    AdminViewPaymentsComponent,
    AdminManageUsersComponent,
    AdminCheckTicketComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    SharedModule,
    AdminRoutingModule
  ]
})
export class AdminModule { }
