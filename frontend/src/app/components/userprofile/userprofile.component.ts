import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Profile } from '../../models/profile.model';
import { ProfileService } from '../../services/profile.service';
import { ROLES } from '../../constants';
import { DOB_EARLIEST, MIN_AGE, birthDateRules, licenseRules, yearsAgoIso } from '../../validation';

// /profile for every role -> ProfileService. a driver fills date of birth + license once, then they are locked
@Component({
  selector: 'app-userprofile',
  templateUrl: './userprofile.component.html'
})
export class UserprofileComponent implements OnInit {

  profile: Profile | null = null;
  profileForm: FormGroup;
  pictures: string[] = [];                          // none or one link, the picture box works with a list
  earliestBirthDate: string = DOB_EARLIEST;
  latestBirthDate: string = yearsAgoIso(MIN_AGE);

  isLoading: boolean = true;
  isSaving: boolean = false;
  errorMessage: string = '';
  successMessage: string = '';

  constructor(private fb: FormBuilder, private profileService: ProfileService) {
    this.profileForm = this.fb.group({
      dateOfBirth: ['', birthDateRules],
      driverLicenseNumber: ['', licenseRules]
    });
    // an answer of the server (license already taken ...) goes away once the driver types again
    this.profileForm.valueChanges.subscribe(() => this.clearMessages());
  }

  ngOnInit(): void {
    this.profileService.getProfile().subscribe({
      next: (profile) => {
        this.isLoading = false;
        this.show(profile);
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Unable to load your profile.';
      }
    });
  }

  get isDriver(): boolean {
    return this.profile?.userRole === ROLES.USER;
  }

  get isLocked(): boolean {
    return this.profile?.profileComplete === true;
  }

  // also runs when the picture changes
  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }

  // a locked form is disabled, so it is never invalid: admins and drivers with a saved profile send only the picture
  save(): void {
    this.clearMessages();
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }
    // the two locked values are not sent again: a license saved before the DL rule must not be checked today
    const firstSave = this.isDriver && !this.isLocked;
    const value = this.profileForm.value;
    this.isSaving = true;
    this.profileService.updateProfile({
      dateOfBirth: firstSave ? value.dateOfBirth : null,
      driverLicenseNumber: firstSave ? value.driverLicenseNumber.trim() : null,
      profilePicture: this.pictures[0] ?? null
    }).subscribe({
      next: (profile) => {
        this.isSaving = false;
        this.show(profile);
        this.successMessage = firstSave ? 'Profile updated. You can book a race now.' : 'Profile updated.';
      },
      error: (err) => {
        this.isSaving = false;
        this.errorMessage = err.error?.message || 'Unable to update your profile.';
      }
    });
  }

  private show(profile: Profile): void {
    this.profile = profile;
    this.pictures = profile.profilePicture ? [profile.profilePicture] : [];
    this.profileForm.patchValue({
      dateOfBirth: profile.dateOfBirth ?? '',
      driverLicenseNumber: profile.driverLicenseNumber ?? ''
    });
    if (profile.profileComplete) {
      this.profileForm.disable();
    }
  }
}
