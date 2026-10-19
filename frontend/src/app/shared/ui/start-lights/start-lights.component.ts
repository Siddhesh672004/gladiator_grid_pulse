import { Component, Input } from '@angular/core';
import { byIndex } from '../../utils/track-by';

// the 5 start lights: "lit" of them are red (competition phase, steps of a form)
@Component({
  selector: 'app-startlights',
  templateUrl: './start-lights.component.html'
})
export class StartLightsComponent {

  @Input() lit: number = 0;
  @Input() total: number = 5;

  readonly byIndex = byIndex;

  // one entry per light: true = red, false = dark
  get lights(): boolean[] {
    const lights: boolean[] = [];
    for (let i = 0; i < this.total; i++) {
      lights.push(i < this.lit);
    }
    return lights;
  }
}
