import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { UserRoutingModule } from './user-routing.module';
import { SharedModule } from '../shared/shared.module';
import { UserviewtracksComponent } from '../components/userviewtracks/userviewtracks.component';
import { UserregisterraceComponent } from '../components/userregisterrace/userregisterrace.component';
import { UserviewmyregistrationsComponent } from '../components/userviewmyregistrations/userviewmyregistrations.component';
import { UseraddfeedbackComponent } from '../components/useraddfeedback/useraddfeedback.component';
import { UserviewfeedbackComponent } from '../components/userviewfeedback/userviewfeedback.component';
import { UserlivetimingComponent } from '../components/userlivetiming/userlivetiming.component';

// lazy loaded driver pages, the component files stay in app/components (My Stats is standalone, see the routing)
@NgModule({
  declarations: [
    UserviewtracksComponent,
    UserregisterraceComponent,
    UserviewmyregistrationsComponent,
    UseraddfeedbackComponent,
    UserviewfeedbackComponent,
    UserlivetimingComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    SharedModule,
    UserRoutingModule
  ]
})
export class UserModule { }
