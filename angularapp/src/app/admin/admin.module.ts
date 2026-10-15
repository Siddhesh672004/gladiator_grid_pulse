import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { AdminRoutingModule } from './admin-routing.module';
import { SharedModule } from '../shared/shared.module';
import { AdminaddtrackComponent } from '../components/adminaddtrack/adminaddtrack.component';
import { TrackformComponent } from '../components/trackform/trackform.component';
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
import { AdmindashboardComponent } from '../components/admindashboard/admindashboard.component';
import { AdminmanageusersComponent } from '../components/adminmanageusers/adminmanageusers.component';
import { AdmincheckticketComponent } from '../components/admincheckticket/admincheckticket.component';

// lazy loaded admin pages, the component files stay in app/components
@NgModule({
  declarations: [
    AdminaddtrackComponent,
    TrackformComponent,
    AdmineditrackComponent,
    AdminviewtracksComponent,
    AdminaddsessionComponent,
    AdminviewsessionsComponent,
    AdmincompetitioncontrolComponent,
    AdminviewregistrationsComponent,
    AdminviewfeedbackComponent,
    AdminracecontrolComponent,
    SuperadminmanageadminsComponent,
    AdminviewpaymentsComponent,
    AdmindashboardComponent,
    AdminmanageusersComponent,
    AdmincheckticketComponent
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
