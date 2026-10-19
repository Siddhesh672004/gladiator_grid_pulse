import { CommonModule } from '@angular/common';
import { Component, ElementRef, HostListener, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { RouterModule } from '@angular/router';
import { Subscription } from 'rxjs';
import {
  ArcElement, BarController, BarElement, CategoryScale, Chart, ChartConfiguration, DoughnutController, Legend, LinearScale,
  LineController, LineElement, PieController, PointElement, Tooltip
} from 'chart.js';
import { AdminDashboard, LiveRace, RecentRegistration } from '../models/admin-dashboard.model';
import { AdminDashboardService } from '../services/admin-dashboard.service';
import { SharedModule } from '../../shared/shared.module';
import { LIVE_REFRESH_MS } from '../../core/constants';
import { byKey } from '../../shared/utils/track-by';
import { errorMessage } from '../../shared/utils/error-message';

// chart.js needs the chart types and parts of this page switched on once (only these are downloaded), and gets our font
Chart.register(DoughnutController, PieController, LineController, BarController, ArcElement, LineElement, PointElement,
  BarElement, CategoryScale, LinearScale, Legend, Tooltip);
Chart.defaults.font.family = 'Archivo, "Segoe UI", sans-serif';
Chart.defaults.color = '#696c74';

// same colours as styles.css
const ORANGE = '#ff5b14';
const INK = '#17181c';
const GREY = '#b9bbc0';
const GREEN = '#1e8e4e';
const YELLOW = '#ffd60a';
const SLICES = [INK, ORANGE, GREY, YELLOW, GREEN, '#1d5fd6'];

// admin home (/admin/dashboard): cards, charts, recent registrations, live race monitor, statement -> AdminDashboardService
// standalone + its own lazy route, so chart.js is only downloaded for this page (and My Stats)
@Component({
  selector: 'app-admindashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, SharedModule],
  templateUrl: './admin-dashboard.component.html'
})
export class AdminDashboardComponent implements OnInit, OnDestroy {

  // the four <canvas> tags of the page (they are always there, also before the numbers arrive)
  @ViewChild('statusChart', { static: true }) statusCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('categoryChart', { static: true }) categoryCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('revenueChart', { static: true }) revenueCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('bookingChart', { static: true }) bookingCanvas!: ElementRef<HTMLCanvasElement>;

  dashboard: AdminDashboard | null = null;
  liveRaces: LiveRace[] = [];
  isLoading: boolean = true;
  errorMessage: string = '';

  private charts: Chart[] = [];
  private dashboardRequest?: Subscription;
  private liveTimer: number = 0;
  private liveRequest?: Subscription;

  readonly byRace = byKey<LiveRace>('sessionId');
  readonly byBooking = byKey<RecentRegistration>('raceBookingId');

  constructor(private dashboardService: AdminDashboardService) { }

  ngOnInit(): void {
    this.load();
    // the live race monitor: now, then every 4 s (a hidden tab skips the rounds and reads it again when shown)
    this.loadLiveRaces();
    this.liveTimer = window.setInterval(() => {
      if (!document.hidden) {
        this.loadLiveRaces();
      }
    }, LIVE_REFRESH_MS);
  }

  @HostListener('document:visibilitychange')
  tabShown(): void {
    if (!document.hidden) {
      this.loadLiveRaces();
    }
  }

  ngOnDestroy(): void {
    window.clearInterval(this.liveTimer);
    this.liveRequest?.unsubscribe();
    this.dashboardRequest?.unsubscribe();
    this.destroyCharts();
  }

  // also the Refresh button
  load(): void {
    this.errorMessage = '';
    this.dashboardRequest = this.dashboardService.getDashboard().subscribe({
      next: (dashboard) => {
        this.dashboard = dashboard;
        this.isLoading = false;
        this.drawCharts(dashboard);
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = errorMessage(err, 'Unable to load the dashboard');
      }
    });
  }

  private loadLiveRaces(): void {
    // an older answer that is still on its way is dropped
    this.liveRequest?.unsubscribe();
    this.liveRequest = this.dashboardService.getLiveRaces().subscribe({
      next: (races) => this.liveRaces = races,
      error: () => { }     // not reachable for a moment: keep the list, the next round tries again
    });
  }

  // false = no money kept in any month of this year (the chart then says so in words)
  get hasRevenue(): boolean {
    if (!this.dashboard) {
      return false;
    }
    return this.dashboard.statement.monthlyRevenue.some(point => point.value > 0);
  }

  // 0 - 100 for the progress bar of a live race (a practice session has no lap limit -> 0)
  progress(race: LiveRace): number {
    return race.progressPercentage ?? 0;
  }

  // the old charts are thrown away first, a canvas can hold only one chart
  private drawCharts(dashboard: AdminDashboard): void {
    this.destroyCharts();
    this.charts = [
      this.statusChart(dashboard),
      this.categoryChart(dashboard),
      this.revenueChart(dashboard),
      this.bookingChart(dashboard)
    ];
  }

  // driver accounts: active / pending / inactive
  private statusChart(dashboard: AdminDashboard): Chart {
    return this.makeChart(this.statusCanvas, {
      type: 'doughnut',
      data: {
        labels: dashboard.registrationStatus.map(point => point.label),
        datasets: [{ data: dashboard.registrationStatus.map(point => point.value), backgroundColor: [GREEN, YELLOW, GREY] }]
      },
      options: { maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } }
    });
  }

  // tracks per category
  private categoryChart(dashboard: AdminDashboard): Chart {
    return this.makeChart(this.categoryCanvas, {
      type: 'pie',
      data: {
        labels: dashboard.trackCategories.map(point => point.label),
        datasets: [{ data: dashboard.trackCategories.map(point => point.value), backgroundColor: SLICES }]
      },
      options: { maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } }
    });
  }

  // money kept per month of this year
  private revenueChart(dashboard: AdminDashboard): Chart {
    const months = dashboard.statement.monthlyRevenue;
    return this.makeChart(this.revenueCanvas, {
      type: 'line',
      data: {
        labels: months.map(point => point.label),
        datasets: [{ label: 'Revenue (₹)', data: months.map(point => point.value),
          borderColor: ORANGE, backgroundColor: ORANGE, tension: 0.3, cubicInterpolationMode: 'monotone' }]
      },
      options: { maintainAspectRatio: false, scales: { y: { beginAtZero: true } } }
    });
  }

  // bookings and races per month of this year
  private bookingChart(dashboard: AdminDashboard): Chart {
    const months = dashboard.bookingRaceStatistics;
    return this.makeChart(this.bookingCanvas, {
      type: 'bar',
      data: {
        labels: months.map(month => month.label),
        datasets: [
          { label: 'Bookings', data: months.map(month => month.bookings), backgroundColor: ORANGE },
          { label: 'Races', data: months.map(month => month.races), backgroundColor: INK }
        ]
      },
      options: { maintainAspectRatio: false, scales: { y: { beginAtZero: true, ticks: { precision: 0 } } } }
    });
  }

  private makeChart(canvas: ElementRef<HTMLCanvasElement>, config: ChartConfiguration): Chart {
    return new Chart(canvas.nativeElement, config);
  }

  private destroyCharts(): void {
    for (const chart of this.charts) {
      chart.destroy();
    }
    this.charts = [];
  }
}
