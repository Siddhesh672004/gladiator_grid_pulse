import { Component, Input } from '@angular/core';

// the 5 start lights: "lit" of them are red (competition phase, steps of a form)
@Component({
  selector: 'app-startlights',
  templateUrl: './startlights.component.html'
})
export class StartlightsComponent {

  @Input() lit: number = 0;
  @Input() total: number = 5;

  // one entry per light: true = red, false = dark
  get lights(): boolean[] {
    const lights: boolean[] = [];
    for (let i = 0; i < this.total; i++) {
      lights.push(i < this.lit);
    }
    return lights;
  }
}
