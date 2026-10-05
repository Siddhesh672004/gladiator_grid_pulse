import { Directive, ElementRef, OnDestroy, OnInit } from '@angular/core';

// <section appReveal>: the block fades up the first time it scrolls into view (css: .on-scroll / .seen in styles.css)
@Directive({
  selector: '[appReveal]'
})
export class RevealDirective implements OnInit, OnDestroy {

  private observer?: IntersectionObserver;

  constructor(private element: ElementRef<HTMLElement>) { }

  ngOnInit(): void {
    const block = this.element.nativeElement;
    block.classList.add('on-scroll');
    this.observer = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting) {
        block.classList.add('seen');
        this.observer?.disconnect();
      }
    }, { threshold: 0.1 });
    this.observer.observe(block);
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
  }
}
