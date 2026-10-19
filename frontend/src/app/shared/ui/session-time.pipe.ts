import { Pipe, PipeTransform } from '@angular/core';
import { RaceSession, sessionTime } from '../models/session.model';

// {{ session | sessionTime }} -> "16:00 - 17:00"
@Pipe({
  name: 'sessionTime',
  standalone: true
})
export class SessionTimePipe implements PipeTransform {

  transform(session: RaceSession): string {
    return sessionTime(session);
  }
}
