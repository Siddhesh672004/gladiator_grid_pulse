import { AfterViewInit, Component, ElementRef, OnDestroy, OnInit } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';
import { RaceService } from '../../core/services/race.service';
import { CompetitionService } from '../../core/services/competition.service';
import { StatsService } from '../../core/services/stats.service';
import { LeaderboardEntry } from '../../shared/models/stats.model';
import { RaceTrack } from '../../shared/models/race-track.model';
import { raceToday } from '../../shared/models/session.model';
import { COMPETITION_STATUS } from '../../core/constants';
import { PHOTOS } from '../photos';
import { SiteSection } from '../site-head/site-head.component';
import { byIndex, byKey } from '../../shared/utils/track-by';
import { scrollBehavior } from '../../shared/utils/motion';

// shown on a number while its call is on the way / when it failed
const LOADING = '…';
const NO_DATA = '—';

// the sections the top bar can jump to (ids of the <section> tags)
const SECTIONS: SiteSection[] = [
  { id: 'tracks', label: 'Tracks' },
  { id: 'raceday', label: 'Race day' },
  { id: 'control', label: 'Race control' },
  { id: 'rules', label: 'Rulebook' },
  { id: 'leaderboard', label: 'Leaderboard' }
];

// same numbers as CompetitionRuleServiceImpl in spring
const RULES = [
  { category: 'Circuit', qualifyingLaps: 3, raceLaps: 15, maxCars: 20 },
  { category: 'Karting', qualifyingLaps: 5, raceLaps: 12, maxCars: 12 },
  { category: 'Rally', qualifyingLaps: 1, raceLaps: 3, maxCars: 10 },
  { category: 'Drag Strip', qualifyingLaps: 1, raceLaps: 1, maxCars: 8 }
];

// one more start light per step
const RACE_DAY = [
  { title: 'Create your account', text: 'A 6-digit code checks your email.' },
  { title: 'Book and pay', text: 'Pick a session and a car number.' },
  { title: 'Get approved', text: 'An admin confirms your seat.' },
  { title: 'Race', text: 'Every lap lands on the live board.' },
  { title: 'Rate the session', text: 'Stars and a few words.' }
];

// made-up rows, only to show what the timing board looks like
const SAMPLE_BOARD = [
  { driver: 'Aarav Rao', car: '27', laps: 7, lastLap: '1:42.306' },
  { driver: 'Meera Nair', car: '08', laps: 7, lastLap: '1:42.911' },
  { driver: 'Kabir Shah', car: '14', laps: 6, lastLap: '1:43.480' },
  { driver: 'Isha Verma', car: '31', laps: 6, lastLap: '1:44.072' }
];

// the public start page: what GridPulse does, the tracks open right now (public api), and the way to sign up
@Component({
  selector: 'app-landing',
  templateUrl: './landing.component.html'
})
export class LandingComponent implements OnInit, AfterViewInit, OnDestroy {

  photo = PHOTOS.race;
  sections = SECTIONS;
  rules = RULES;
  steps = RACE_DAY;
  board = SAMPLE_BOARD;

  current: string = '';             // id of the section on screen, marked in the top bar
  tracks: RaceTrack[] = [];         // tracks with a session open for booking, the first 4
  isLoading: boolean = true;
  trackCount: string = LOADING;
  sessionCount: string = LOADING;
  competitionCount: string = LOADING;
  leaders: LeaderboardEntry[] = [];  // top 5 drivers (public api)
  leadersLoaded: boolean = false;

  loggedIn: boolean = false;
  isDriver: boolean = false;
  startLink: string = '/signup';    // where the main button goes for this visitor
  startText: string = 'Book a session';

  private observer?: IntersectionObserver;

  readonly byIndex = byIndex;
  readonly byTrack = byKey<RaceTrack>('trackId');

  constructor(private authService: AuthService, private raceService: RaceService,
              private competitionService: CompetitionService, private statsService: StatsService,
              private host: ElementRef<HTMLElement>) { }

  ngOnInit(): void {
    this.setStartButton();
    this.loadTracks();
    this.loadCompetitions();
    this.loadLeaders();
  }

  // the main button: a visitor signs up, a driver goes to the tracks, an admin to the dashboard
  private setStartButton(): void {
    this.loggedIn = this.authService.isAuthenticated();
    this.isDriver = this.loggedIn && this.authService.isCustomer();
    if (this.isDriver) {
      this.startLink = '/user/view/tracks';
    } else if (this.loggedIn) {
      this.startLink = '/admin/dashboard';
      this.startText = 'Open my dashboard';
    }
  }

  // the section crossing the middle of the window is the current one
  ngAfterViewInit(): void {
    this.observer = new IntersectionObserver(entries => {
      for (const entry of entries) {
        if (entry.isIntersecting) {
          this.current = entry.target.id;
        }
      }
    }, { rootMargin: '-45% 0px -50% 0px' });
    this.host.nativeElement.querySelectorAll('.screen').forEach(screen => this.observer?.observe(screen));
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
  }

  goTo(id: string): void {
    document.getElementById(id)?.scrollIntoView({ behavior: scrollBehavior() });
  }

  // same tracks as the driver's View Tracks: active and with a session still open
  private loadTracks(): void {
    this.raceService.getAllTracks().subscribe({
      next: (all) => {
        const open = all.filter(track => track.active && (track.upcomingSessions ?? 0) > 0);
        this.tracks = open.slice(0, 4);
        this.trackCount = String(open.length);
        let sessions = 0;
        for (const track of open) {
          sessions += track.upcomingSessions ?? 0;
        }
        this.sessionCount = String(sessions);
        this.isLoading = false;
      },
      error: () => {
        this.trackCount = NO_DATA;
        this.sessionCount = NO_DATA;
        this.isLoading = false;
      }
    });
  }

  // competitions not started yet, today or later (India time)
  private loadCompetitions(): void {
    const today = raceToday();
    this.competitionService.getAllCompetitions().subscribe({
      next: (all) => {
        const coming = all.filter(competition =>
          competition.status === COMPETITION_STATUS.UPCOMING && competition.competitionDate >= today);
        this.competitionCount = String(coming.length);
      },
      error: () => this.competitionCount = NO_DATA
    });
  }

  // wins first, then podiums, then laps (the backend sorts)
  private loadLeaders(): void {
    this.statsService.getLeaderboard(5).subscribe({
      next: (leaders) => {
        this.leaders = leaders;
        this.leadersLoaded = true;
      },
      error: () => this.leadersLoaded = true
    });
  }

  // a driver goes straight to the booking page of that track, everyone else logs in first
  bookLink(track: RaceTrack): string {
    if (this.isDriver) {
      return '/user/register/' + track.trackId;
    }
    return '/login';
  }
}
