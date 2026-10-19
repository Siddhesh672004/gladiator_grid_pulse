import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { RaceTrack } from '../../shared/models/race-track.model';
import { RaceService } from '../../core/services/race.service';
import { pageOf } from '../../shared/utils/paging';
import { FieldOption, toOptions } from '../../shared/utils/field-option';
import { byKey } from '../../shared/utils/track-by';
import { errorMessage } from '../../shared/utils/error-message';

// admin track table: search, indoor / outdoor filter, edit, activate / deactivate, delete -> RaceService
// a track that ever had a session keeps its history: it can only be deactivated, not deleted
@Component({
  selector: 'app-adminviewtracks',
  templateUrl: './admin-view-tracks.component.html'
})
export class AdminViewTracksComponent implements OnInit {

  tracks: RaceTrack[] = [];
  searchText: string = '';
  filterType: string = 'All';
  typeOptions: FieldOption[] = toOptions(['Indoor', 'Outdoor']);
  page: number = 1;
  pageSize: number = 10;
  isLoading: boolean = true;
  errorMessage: string = '';
  successMessage: string = '';
  trackToDelete: RaceTrack | null = null;

  readonly byTrack = byKey<RaceTrack>('trackId');

  constructor(private raceService: RaceService, private router: Router) { }

  ngOnInit(): void {
    this.loadTracks();
  }

  loadTracks(): void {
    this.raceService.getAllTracks().subscribe({
      next: (tracks) => {
        // newest first
        this.tracks = tracks.sort((a, b) => (b.trackId ?? 0) - (a.trackId ?? 0));
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.errorMessage = 'Unable to load tracks';
      }
    });
  }

  // the rows left after the search box and the indoor / outdoor filter
  get filteredTracks(): RaceTrack[] {
    const text = this.searchText.trim().toLowerCase();
    return this.tracks.filter(track => this.hasText(track, text) && this.hasType(track));
  }

  private hasText(track: RaceTrack, text: string): boolean {
    return track.trackName.toLowerCase().includes(text) || track.description.toLowerCase().includes(text);
  }

  private hasType(track: RaceTrack): boolean {
    if (this.filterType === 'All') {
      return true;
    }
    if (this.filterType === 'Indoor') {
      return track.isIndoor === true;
    }
    return track.isIndoor === false;
  }

  get pagedTracks(): RaceTrack[] {
    return pageOf(this.filteredTracks, this.page, this.pageSize);
  }

  editTrack(trackId: number): void {
    this.router.navigate(['/admin/edit/track', trackId]);
  }

  // Deactivate hides the track from drivers, Activate brings it back
  toggleActive(track: RaceTrack): void {
    this.errorMessage = '';
    this.successMessage = '';
    if (track.active) {
      this.raceService.deactivateTrack(track.trackId!).subscribe({
        next: (updated) => this.showChanged(updated),
        error: (err) => this.showChangeError(err)
      });
    } else {
      this.raceService.activateTrack(track.trackId!).subscribe({
        next: (updated) => this.showChanged(updated),
        error: (err) => this.showChangeError(err)
      });
    }
  }

  // put the answer of the backend in place of the old row
  private showChanged(updated: RaceTrack): void {
    const index = this.tracks.findIndex(track => track.trackId === updated.trackId);
    if (index >= 0) {
      this.tracks[index] = updated;
    }
    if (updated.active) {
      this.successMessage = `"${updated.trackName}" is now active`;
    } else {
      this.successMessage = `"${updated.trackName}" is now inactive`;
    }
  }

  // 409 -> the track still has upcoming sessions, the backend explains it
  private showChangeError(err: any): void {
    this.errorMessage = errorMessage(err, 'Unable to change the track');
  }

  askDelete(track: RaceTrack): void {
    this.errorMessage = '';
    this.successMessage = '';
    this.trackToDelete = track;
  }

  // "Yes, Delete" of the popup
  deleteTrack(): void {
    const track = this.trackToDelete;
    this.trackToDelete = null;
    if (!track?.trackId) {
      return;
    }
    this.raceService.deleteTrack(track.trackId).subscribe({
      next: () => {
        this.successMessage = `"${track.trackName}" was deleted`;
        this.tracks = this.tracks.filter(row => row.trackId !== track.trackId);
      },
      error: (err) => {
        // 409 -> the track has sessions (history), the backend says "deactivate it instead"
        this.errorMessage = errorMessage(err, 'Unable to delete the track');
      }
    });
  }
}
