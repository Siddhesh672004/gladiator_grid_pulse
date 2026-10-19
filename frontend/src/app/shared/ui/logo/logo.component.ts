import { ChangeDetectionStrategy, Component } from '@angular/core';

// the GridPulse logo: the pulse line + the name (navbars, login pages, landing page)
@Component({
  selector: 'app-logo',
  standalone: true,
  templateUrl: './logo.component.html',
  // nothing in it ever changes
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LogoComponent { }
