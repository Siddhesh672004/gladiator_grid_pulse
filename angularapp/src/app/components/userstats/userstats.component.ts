import { Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Chart, registerables } from 'chart.js';
import { DriverStats } from '../../models/stats.model';
import { StatsService } from '../../services/stats.service';

// chart.js needs its chart types switched on once (the admin pages do the same in their own bundle)
Chart.register(...registerables);
Chart.defaults.font.family = 'Archivo, "Segoe UI", sans-serif';
Chart.defaults.color = '#696c74';
const ORANGE = '#ff5b14';

// My Stats (/user/stats): number tiles + laps per month of this year -> StatsService. completed sessions only
@Component({
  selector: 'app-userstats',
  templateUrl: './userstats.component.html'
})
export class UserstatsComponent implements OnInit, OnDestroy {

  // the canvas is always in the page, the chart is drawn when the numbers arrive
  @ViewChild('lapsChart', { static: true }) lapsCanvas!: ElementRef<HTMLCanvasElement>;

  stats: DriverStats | null = null;
  isLoading: boolean = true;
  errorMessage: string = '';
  private chart?: Chart;

  constructor(private statsService: StatsService) { }

  ngOnInit(): void {
    this.statsService.getMyStats().subscribe({
      next: (stats) => {
        this.stats = stats;
        this.isLoading = false;
        this.drawChart(stats);
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Unable to load your stats';
      }
    });
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
  }

  // false = no lap in any month of this year (the chart then says so in words)
  get hasLaps(): boolean {
    return !!this.stats && this.stats.lapsPerMonth.some(month => month.value > 0);
  }

  // P1, P3 ... or "-" before the first race / qualifying
  place(position: number | null): string {
    return position ? 'P' + position : '-';
  }

  private drawChart(stats: DriverStats): void {
    this.chart?.destroy();
    this.chart = new Chart(this.lapsCanvas.nativeElement, {
      type: 'bar',
      data: {
        labels: stats.lapsPerMonth.map(month => month.label),
        datasets: [{ label: 'Laps', data: stats.lapsPerMonth.map(month => month.value), backgroundColor: ORANGE }]
      },
      options: { maintainAspectRatio: false, scales: { y: { beginAtZero: true, ticks: { precision: 0 } } } }
    });
  }
}
