import { Pipe, PipeTransform } from '@angular/core';
import { RaceSession, sessionLabel } from '../models/session.model';

// {{ session | sessionLabel }} -> "Practice" or "Summer Cup - Qualifying"
@Pipe({
  name: 'sessionLabel',
  standalone: true
})
export class SessionLabelPipe implements PipeTransform {

  transform(session: RaceSession): string {
    return sessionLabel(session);
  }
}
