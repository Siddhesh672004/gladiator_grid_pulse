import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ValidatorFn, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { RaceTrack } from '../../shared/models/race-track.model';
import { SessionRequest } from '../../shared/models/session.model';
import { CompetitionRequest, CompetitionRule } from '../../shared/models/competition.model';
import { RaceService } from '../../core/services/race.service';
import { SessionService } from '../../core/services/session.service';
import { CompetitionService } from '../../core/services/competition.service';
import { SESSION_TYPE } from '../../core/constants';
import { DIGITS_PATTERN, HAS_LETTER_PATTERN, PRICE_MAX, after, matches, maxChars, minChars, minMinutesAfter, monthsAheadIso,
  notBefore, notInPast, notPassedToday, recheckWhenChanged, required, todayIso, withinMonths } from '../../shared/utils/validation';
import { byKey } from '../../shared/utils/track-by';

const DATE_RANGE = 'Date must be within the next 12 months';
const START_PASSED = 'Start Time has already passed';
const TOO_SHORT = 'A session must be at least 30 minutes long';

// Add Session (/admin/add/session): Practice (one slot) or Competition (qualifying + race) -> SessionService / CompetitionService
// the same page is Edit Session (/admin/edit/session/:id) for a practice session that has not started
@Component({
  selector: 'app-adminaddsession',
  templateUrl: './admin-add-session.component.html'
})
export class AdminAddSessionComponent implements OnInit {

  tracks: RaceTrack[] = [];
  selectedTrackId: number | null = null;
  sessionType: string = SESSION_TYPE.PRACTICE;
  types = SESSION_TYPE;
  today: string = todayIso();
  lastDay: string = monthsAheadIso(12);

  practiceForm: FormGroup;
  competitionForm: FormGroup;
  rule: CompetitionRule | null = null;

  editId: number | null = null;                   // set = this page edits an existing practice session
  savedSlot: Record<string, string> | null = null; // edit: date + times as they were saved
  createdCompetitionId: number | null = null;
  submitted: boolean = false;
  isSaving: boolean = false;
  errorMessage: string = '';
  successText: string = '';

  readonly byTrack = byKey<RaceTrack>('trackId');

  constructor(private fb: FormBuilder, private raceService: RaceService, private sessionService: SessionService,
              private competitionService: CompetitionService, private route: ActivatedRoute, private router: Router) {
    this.practiceForm = this.buildPracticeForm();
    this.competitionForm = this.buildCompetitionForm();
  }

  private buildPracticeForm(): FormGroup {
    const form = this.fb.group({
      sessionDate: ['', [required, notInPast('Date cannot be in the past'),
        this.unlessKept(['sessionDate'], withinMonths(12, DATE_RANGE))]],
      startTime: ['', [required, this.unlessKept(['sessionDate', 'startTime'], notPassedToday('sessionDate', START_PASSED)),
        this.insideOpenHours()]],
      endTime: ['', [required, after('startTime', 'End Time must be after Start Time'),
        this.unlessKept(['startTime', 'endTime'], minMinutesAfter('startTime', 30, TOO_SHORT)), this.insideOpenHours()]],
      price: [null, [required, Validators.min(1), Validators.max(PRICE_MAX), matches(DIGITS_PATTERN, 'Price can only have digits')]],
      maxCars: [10, [required, Validators.min(1), Validators.max(100)]]
    });
    // a new date / start time can make the next box wrong
    recheckWhenChanged(form, 'sessionDate', 'startTime');
    recheckWhenChanged(form, 'startTime', 'endTime');
    return form;
  }

  // edit: times that were not changed keep their old slot, the new-slot rules are for new times only
  private unlessKept(fields: string[], rule: ValidatorFn): ValidatorFn {
    return (control) => {
      const saved = this.savedSlot;
      if (saved && fields.every(field => control.parent?.get(field)?.value === saved[field])) {
        return null;
      }
      return rule(control);
    };
  }

  // a time outside the opening hours of the chosen track, same text as the backend
  private insideOpenHours(): ValidatorFn {
    return (control) => {
      const track = this.selectedTrack;
      if (!control.value || !track) {
        return null;
      }
      const open = track.openTime.substring(0, 5);
      const close = track.closeTime.substring(0, 5);
      if (control.value >= open && control.value <= close) {
        return null;
      }
      return { openHours: `Session must be within the track's operating hours (${open} - ${close})` };
    };
  }

  // the opening hours come from the track: another track can make a time right or wrong
  private recheckTimes(): void {
    for (const form of [this.practiceForm, this.competitionForm]) {
      for (const box of Object.values(form.controls)) {
        box.updateValueAndValidity({ emitEvent: false });
      }
    }
  }

  private buildCompetitionForm(): FormGroup {
    const form = this.fb.group({
      competitionName: ['', [required, minChars(3), maxChars(100), matches(HAS_LETTER_PATTERN, 'Competition Name must have a letter')]],
      competitionDate: ['', [required, notInPast('Date cannot be in the past'), withinMonths(12, DATE_RANGE)]],
      qualifyingStartTime: ['', [required, notPassedToday('competitionDate', START_PASSED), this.insideOpenHours()]],
      qualifyingEndTime: ['', [required, after('qualifyingStartTime', 'End Time must be after Start Time'),
        minMinutesAfter('qualifyingStartTime', 30, TOO_SHORT), this.insideOpenHours()]],
      raceStartTime: ['', [required, notPassedToday('competitionDate', START_PASSED), this.insideOpenHours(),
        notBefore('qualifyingEndTime', 'The race must start after qualifying has ended')]],
      raceEndTime: ['', [required, after('raceStartTime', 'End Time must be after Start Time'),
        minMinutesAfter('raceStartTime', 30, TOO_SHORT), this.insideOpenHours()]],
      entryFee: [null, [required, Validators.min(1), Validators.max(PRICE_MAX),
        matches(DIGITS_PATTERN, 'Entry fee can only have digits')]]
    });
    recheckWhenChanged(form, 'competitionDate', 'qualifyingStartTime');
    recheckWhenChanged(form, 'competitionDate', 'raceStartTime');
    recheckWhenChanged(form, 'qualifyingStartTime', 'qualifyingEndTime');
    recheckWhenChanged(form, 'qualifyingEndTime', 'raceStartTime');
    recheckWhenChanged(form, 'raceStartTime', 'raceEndTime');
    return form;
  }

  ngOnInit(): void {
    this.raceService.getAllTracks().subscribe({
      next: (tracks) => {
        this.tracks = tracks.filter(track => track.active);
        this.recheckTimes();
      },
      error: () => this.errorMessage = 'Unable to load tracks'
    });
    // /admin/edit/session/7 -> fill the form with session 7
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.editId = Number(id);
      this.loadSessionToEdit(this.editId);
    }
  }

  get isEdit(): boolean {
    return this.editId !== null;
  }

  get isPractice(): boolean {
    return this.sessionType === SESSION_TYPE.PRACTICE;
  }

  get selectedTrack(): RaceTrack | undefined {
    return this.tracks.find(track => track.trackId === this.selectedTrackId);
  }

  // "06:00 - 23:00" of the chosen track, for the box on the right
  get openHours(): string {
    const track = this.selectedTrack;
    if (!track) {
      return '-';
    }
    return track.openTime.substring(0, 5) + ' - ' + track.closeTime.substring(0, 5);
  }

  // the Track box or the Practice / Competition choice changed
  onTrackOrTypeChange(): void {
    this.errorMessage = '';
    this.recheckTimes();
    this.loadRule();
  }

  // competition: show which rule the backend will use for this track
  private loadRule(): void {
    this.rule = null;
    if (this.isPractice || !this.selectedTrackId) {
      return;
    }
    this.competitionService.getRuleForTrack(this.selectedTrackId).subscribe({
      next: (rule) => this.rule = rule,
      error: (err) => this.errorMessage = err.error?.message || 'Unable to load the competition rule'
    });
  }

  private loadSessionToEdit(sessionId: number): void {
    this.sessionService.getSessionById(sessionId).subscribe({
      next: (session) => {
        if (session.sessionType !== SESSION_TYPE.PRACTICE) {
          this.errorMessage = 'A competition session cannot be edited here - open the competition instead.';
          return;
        }
        this.selectedTrackId = session.trackId;
        // backend times look like "07:00:00", the time box wants "07:00"
        this.savedSlot = {
          sessionDate: session.sessionDate,
          startTime: session.startTime.substring(0, 5),
          endTime: session.endTime.substring(0, 5)
        };
        this.practiceForm.patchValue({ ...this.savedSlot, price: session.price, maxCars: session.maxCars });
      },
      error: () => this.errorMessage = 'This session does not exist any more.'
    });
  }

  // Submit of both forms. "submitted" is only for the Track box (it is not part of the two forms)
  save(): void {
    this.submitted = true;
    this.errorMessage = '';
    if (this.isPractice) {
      if (this.practiceForm.invalid || !this.selectedTrackId) {
        this.practiceForm.markAllAsTouched();
        return;
      }
      this.savePractice(this.selectedTrackId);
    } else {
      if (this.competitionForm.invalid || !this.selectedTrackId) {
        this.competitionForm.markAllAsTouched();
        return;
      }
      this.saveCompetition(this.selectedTrackId);
    }
  }

  private savePractice(trackId: number): void {
    const value = this.practiceForm.value;
    const request: SessionRequest = {
      trackId: trackId,
      sessionDate: value.sessionDate,
      startTime: value.startTime,
      endTime: value.endTime,
      price: Number(value.price),
      maxCars: Number(value.maxCars)
    };
    this.isSaving = true;
    if (this.editId !== null) {
      this.sessionService.updatePracticeSession(this.editId, request).subscribe({
        next: () => this.onSaved('Session updated successfully!'),
        error: (err) => this.onFailed(err)
      });
    } else {
      this.sessionService.addPracticeSession(request).subscribe({
        next: () => this.onSaved('Practice session created!'),
        error: (err) => this.onFailed(err)
      });
    }
  }

  private saveCompetition(trackId: number): void {
    const value = this.competitionForm.value;
    const request: CompetitionRequest = {
      trackId: trackId,
      competitionName: value.competitionName.trim(),
      competitionDate: value.competitionDate,
      qualifyingStartTime: value.qualifyingStartTime,
      qualifyingEndTime: value.qualifyingEndTime,
      raceStartTime: value.raceStartTime,
      raceEndTime: value.raceEndTime,
      entryFee: Number(value.entryFee)
    };
    this.isSaving = true;
    this.competitionService.createCompetition(request).subscribe({
      next: (competition) => {
        this.createdCompetitionId = competition.competitionId;
        this.onSaved('Competition created with its Qualifying and Race sessions!');
      },
      error: (err) => this.onFailed(err)
    });
  }

  // the success popup shows this text
  private onSaved(text: string): void {
    this.isSaving = false;
    this.successText = text;
  }

  private onFailed(err: any): void {
    this.isSaving = false;
    this.errorMessage = err.error?.message || 'Unable to save. Please try again.';
  }

  // a new competition opens its Competition Control, everything else goes back to the list
  closeModal(): void {
    this.successText = '';
    if (this.createdCompetitionId !== null) {
      this.router.navigate(['/admin/competition', this.createdCompetitionId]);
    } else {
      this.router.navigate(['/admin/view/sessions']);
    }
  }

  goBack(): void {
    this.router.navigate(['/admin/view/sessions']);
  }
}
