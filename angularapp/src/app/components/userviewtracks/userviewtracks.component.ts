import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { RaceTrack } from '../../models/race-track.model';
import { RaceService } from '../../services/race.service';
import { pageOf } from '../../shared/paging';

// track cards -> RaceService, Register opens the booking form
// only tracks with at least one session still open for booking (upcomingSessions from the backend)
@Component({
  selector: 'app-userviewtracks',
  templateUrl: './userviewtracks.component.html'
})
export class UserviewtracksComponent implements OnInit {

  tracks: RaceTrack[] = [];
  searchText: string = '';
  page: number = 1;
  pageSize: number = 12;           // 12 cards fill 2, 3 or 4 columns evenly
  isLoading: boolean = true;
  errorMessage: string = '';

  constructor(private raceService: RaceService, private router: Router) { }

  ngOnInit(): void {
    this.raceService.getAllTracks().subscribe({
      next: (data) => {
        this.tracks = data.filter(track => track.active && (track.upcomingSessions ?? 0) > 0).sort((a, b) => (b.trackId ?? 0) - (a.trackId ?? 0));
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.errorMessage = 'Unable to load tracks';
      }
    });
  }

  get filteredTracks(): RaceTrack[] {
    const text = this.searchText.trim().toLowerCase();
    return this.tracks.filter(track =>
      track.trackName.toLowerCase().includes(text) ||
      track.location.toLowerCase().includes(text) ||
      track.trackCategory.toLowerCase().includes(text));
  }

  get pagedTracks(): RaceTrack[] {
    return pageOf(this.filteredTracks, this.page, this.pageSize);
  }

  register(trackId: number): void {
    this.router.navigate(['/user/register', trackId]);
  }
}
