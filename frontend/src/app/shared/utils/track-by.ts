// trackBy for *ngFor: a list that is drawn again (a poll, another page, a search) keeps the html of the rows it
// already shows instead of building them all again

// fixed lists (texts, steps, lights) and lists that only grow at the end (chat bubbles)
export function byIndex(index: number): number {
  return index;
}

// rows with an id:  readonly byBooking = byKey<RaceBooking>('raceBookingId')
export function byKey<T>(key: keyof T): (index: number, row: T) => unknown {
  return (_index: number, row: T) => row[key];
}
