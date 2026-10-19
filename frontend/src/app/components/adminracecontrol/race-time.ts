import { RACE_TIME_ZONE } from '../../constants';

// one formatter for every clock time on the race boards: backend UTC (ISO-8601) -> India time (RACE_TIME_ZONE)
const RACE_CLOCK_OPTIONS: Intl.DateTimeFormatOptions = {
  timeZone: RACE_TIME_ZONE,
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit'
};
const RACE_CLOCK = new Intl.DateTimeFormat('en-GB', RACE_CLOCK_OPTIONS);

export const NO_TIME = '–';

// missing or unreadable -> "–"
export function formatRaceTime(value: string | Date | null | undefined): string {
  if (!value) {
    return NO_TIME;
  }
  const moment = value instanceof Date ? value : new Date(value);
  return isNaN(moment.getTime()) ? NO_TIME : RACE_CLOCK.format(moment);
}
