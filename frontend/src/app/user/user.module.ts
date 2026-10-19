import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { UserRoutingModule } from './user-routing.module';
import { SharedModule } from '../shared/shared.module';
import { UserViewTracksComponent } from './view-tracks/user-view-tracks.component';
import { UserRegisterRaceComponent } from './register-race/user-register-race.component';
import { UserViewMyRegistrationsComponent } from './view-my-registrations/user-view-my-registrations.component';
import { UserAddFeedbackComponent } from './add-feedback/user-add-feedback.component';
import { UserViewFeedbackComponent } from './view-feedback/user-view-feedback.component';
import { UserLiveTimingComponent } from './live-timing/user-live-timing.component';
import { CarouselComponent } from './carousel/carousel.component';

// lazy loaded driver pages (My Stats is standalone, see the routing)
@NgModule({
  declarations: [
    UserViewTracksComponent,
    UserRegisterRaceComponent,
    UserViewMyRegistrationsComponent,
    UserAddFeedbackComponent,
    UserViewFeedbackComponent,
    UserLiveTimingComponent,
    CarouselComponent
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
