import { Component, Input, OnChanges } from '@angular/core';

// the pictures of a track, one at a time, with previous / next arrows and a dot per picture
@Component({
  selector: 'app-carousel',
  templateUrl: './carousel.component.html'
})
export class CarouselComponent implements OnChanges {

  @Input() images: string[] = [];
  @Input() alt: string = '';
  index: number = 0;

  // another list of pictures (another track) -> start again at the first one
  ngOnChanges(): void {
    this.index = 0;
  }

  previous(): void {
    this.index = (this.index - 1 + this.images.length) % this.images.length;
  }

  next(): void {
    this.index = (this.index + 1) % this.images.length;
  }

  show(i: number): void {
    this.index = i;
  }
}
