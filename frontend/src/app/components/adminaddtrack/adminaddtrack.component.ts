import { Component } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Router } from '@angular/router';
import { RaceService } from '../../services/race.service';
import { buildTrackForm, toTrack } from '../trackform/track-form';

// "Create New Track" page: the boxes are in <app-trackform>, Submit -> RaceService.addTrack -> popup -> track list
@Component({
  selector: 'app-adminaddtrack',
  templateUrl: './adminaddtrack.component.html'
})
export class AdminaddtrackComponent {

  trackForm: FormGroup;
  errorMessage: string = '';
  showModal: boolean = false;
  isSaving: boolean = false;

  constructor(private fb: FormBuilder, private raceService: RaceService, private router: Router) {
    this.trackForm = buildTrackForm(this.fb);
  }

  addTrack(): void {
    this.errorMessage = '';
    if (this.trackForm.invalid) {
      this.trackForm.markAllAsTouched();
      return;
    }
    this.isSaving = true;
    this.raceService.addTrack(toTrack(this.trackForm)).subscribe({
      next: () => {
        this.isSaving = false;
        this.showModal = true;
      },
      error: (err) => {
        this.isSaving = false;
        // 409 duplicate name (or a 400 rule) -> shown inline under the form
        this.errorMessage = err.error?.message || 'Unable to add the track. Please try again.';
      }
    });
  }

  closeModal(): void {
    this.showModal = false;
    this.router.navigate(['/admin/view/tracks']);
  }
}
