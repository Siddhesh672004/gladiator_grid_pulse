import { FormBuilder, FormGroup } from '@angular/forms';
import { RaceTrack } from '../../models/race-track.model';
import { HAS_LETTER_PATTERN, after, matches, maxChars, minChars, recheckWhenChanged, required } from '../../validation';

// the form behind <app-trackform>: Add Track and Edit Track build it here and turn it into a RaceTrack here

export function buildTrackForm(fb: FormBuilder): FormGroup {
  const form = fb.group({
    trackName: ['', [required, minChars(3), maxChars(100), matches(HAS_LETTER_PATTERN, 'Track Name must have a letter')]],
    description: ['', [required, minChars(10), maxChars(1000)]],
    location: ['', [required, minChars(3), maxChars(200), matches(HAS_LETTER_PATTERN, 'Location must have a letter')]],
    trackCategory: ['', [required]],
    isIndoor: [null, [required]],
    openTime: ['06:00', [required]],
    closeTime: ['23:00', [required, after('openTime', 'Closing Time must be after Opening Time')]],
    facilities: ['', [required, minChars(3), maxChars(500)]],
    imageUrls: [[], [required]]
  });
  // a new opening time can make the closing time wrong
  recheckWhenChanged(form, 'openTime', 'closeTime');
  return form;
}

// what the form holds -> what the backend wants (texts without spaces around them)
export function toTrack(form: FormGroup): RaceTrack {
  const value = form.value;
  return {
    trackName: value.trackName.trim(),
    description: value.description.trim(),
    location: value.location.trim(),
    trackCategory: value.trackCategory,
    isIndoor: value.isIndoor,
    openTime: value.openTime,
    closeTime: value.closeTime,
    facilities: value.facilities.trim(),
    imageUrls: value.imageUrls
  };
}
