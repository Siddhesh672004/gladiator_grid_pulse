import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { RaceTrack } from '../../shared/models/race-track.model';
import { RaceService } from '../../core/services/race.service';
import { buildTrackForm, toTrack } from '../track-form/track-form';

// Edit Track (/admin/edit/track/:id): the add-track form filled with the current values -> RaceService.updateTrack
// a track saved before pictures existed has none, the admin must add 1 - 5 before saving
@Component({
  selector: 'app-admineditrack',
  templateUrl: './admin-edit-track.component.html'
})
export class AdminEditTrackComponent implements OnInit {

  trackForm: FormGroup;
  trackId: number = 0;
  isLoading: boolean = true;
  notFound: boolean = false;
  errorMessage: string = '';
  showModal: boolean = false;
  isSaving: boolean = false;

  constructor(private fb: FormBuilder, private raceService: RaceService, private route: ActivatedRoute,
              private router: Router) {
    this.trackForm = buildTrackForm(this.fb);
  }

  ngOnInit(): void {
    this.trackId = Number(this.route.snapshot.paramMap.get('id'));
    this.raceService.getTrackById(this.trackId).subscribe({
      next: (track) => {
        this.fillForm(track);
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        // any load problem: show the message, never an empty form that could be saved
        this.notFound = true;
        this.errorMessage = err.error?.message || 'Unable to load the track.';
      }
    });
  }

  private fillForm(track: RaceTrack): void {
    this.trackForm.patchValue(track);
    // backend times look like "07:00:00", the time box wants "07:00"
    this.trackForm.patchValue({
      openTime: track.openTime?.substring(0, 5),
      closeTime: track.closeTime?.substring(0, 5)
    });
  }

  updateTrack(): void {
    this.errorMessage = '';
    if (this.trackForm.invalid) {
      this.trackForm.markAllAsTouched();
      return;
    }
    this.isSaving = true;
    this.raceService.updateTrack(this.trackId, toTrack(this.trackForm)).subscribe({
      next: () => {
        this.isSaving = false;
        this.showModal = true;
      },
      error: (err) => {
        this.isSaving = false;
        this.errorMessage = err.error?.message || 'Unable to update the track. Please try again.';
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/admin/view/tracks']);
  }

  closeModal(): void {
    this.showModal = false;
    this.goBack();
  }
}
