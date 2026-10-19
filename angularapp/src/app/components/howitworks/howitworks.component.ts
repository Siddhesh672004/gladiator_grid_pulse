import { AfterViewInit, Component, ElementRef, OnDestroy } from '@angular/core';
import { CHAPTERS } from './howitworks.data';
import { byIndex } from '../../shared/track-by';

// public walkthrough: one chapter per screen (text from howitworks.data.ts + a small drawn screen)
// the numbers in the bar on top jump to a chapter and mark the one on screen (handy while presenting)
@Component({
  selector: 'app-howitworks',
  templateUrl: './howitworks.component.html'
})
export class HowitworksComponent implements AfterViewInit, OnDestroy {

  chapters = CHAPTERS;
  // the number bar: the driver's chapters, then the admin's
  groups = [
    { role: 'Driver', chapters: CHAPTERS.filter(chapter => chapter.role === 'Driver') },
    { role: 'Admin', chapters: CHAPTERS.filter(chapter => chapter.role === 'Admin') }
  ];
  current: string = CHAPTERS[0].id;
  bars = [30, 45, 40, 60, 75, 55, 80, 65, 90, 70, 50, 62];     // made-up chart of the dashboard drawing

  private observer?: IntersectionObserver;

  readonly byIndex = byIndex;

  constructor(private host: ElementRef<HTMLElement>) { }

  // the chapter crossing the middle of the window becomes the current one
  ngAfterViewInit(): void {
    this.observer = new IntersectionObserver(entries => {
      for (const entry of entries) {
        if (entry.isIntersecting) {
          this.current = entry.target.id;
        }
      }
    }, { rootMargin: '-45% 0px -50% 0px' });
    this.host.nativeElement.querySelectorAll('.slide').forEach(slide => this.observer?.observe(slide));
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
  }

  goTo(id: string): void {
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' });
  }
}
