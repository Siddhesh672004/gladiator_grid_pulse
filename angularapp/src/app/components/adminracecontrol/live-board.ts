import { LiveStanding } from '../../models/live-standing.model';
import { RACE_STATES } from '../../constants';

// what the three live boards show for one car (Practice Control, Live Timing, Competition Control)
// the ranking itself always comes from the backend

// a car that was never started has no state yet
export function carState(row: LiveStanding): string {
  return row.raceState || RACE_STATES.NOT_STARTED;
}

// position column: finished cars show their final place, retired cars "RETIRED", others their live rank
export function positionText(row: LiveStanding, index: number): string {
  if (carState(row) === RACE_STATES.RETIRED) {
    return 'RETIRED';
  }
  if (carState(row) === RACE_STATES.FINISHED && row.finalPosition) {
    return 'P' + row.finalPosition;
  }
  return String(index + 1);
}

// the first row gets the orange mark, unless that car retired
export function isLeaderRow(row: LiveStanding, index: number): boolean {
  return index === 0 && carState(row) !== RACE_STATES.RETIRED;
}

// the Date box above a board (Practice Control, Live Timing), '' = fine
// a cleared box and an impossible date (31 Feb) both arrive as '', badInput tells them apart
export function pickerDateProblem(box: HTMLInputElement): string {
  if (box.validity.badInput) {
    return 'Please enter a real date';
  }
  if (box.value === '') {
    return 'Date is required';
  }
  return /^\d{4}-\d{2}-\d{2}$/.test(box.value) ? '' : 'Please enter a real date';
}
