import { Component, Input } from '@angular/core';

// a status shown as a race flag: green = go, yellow = wait, red = stop, chequered = finished, blue = coming up
const FLAGS: { [status: string]: string } = {
  Approved: 'green', LIVE: 'green', RACING: 'green', QUALIFYING: 'green', PAID: 'green', Active: 'green', POSITIVE: 'green',
  Pending: 'yellow', Waitlisted: 'yellow', CREATED: 'yellow', CAPTURED: 'yellow', REFUND_PENDING: 'yellow',
  Rejected: 'red', CANCELLED: 'red', FAILED: 'red', REFUND_FAILED: 'red', Inactive: 'red', NEGATIVE: 'red',
  COMPLETED: 'chequered', FINISHED: 'chequered', QUALIFYING_COMPLETED: 'chequered',
  SCHEDULED: 'blue', UPCOMING: 'blue', REFUNDED: 'blue'
};

@Component({
  selector: 'app-flag',
  templateUrl: './flag.component.html'
})
export class FlagComponent {

  @Input() status: string = '';
  @Input() label: string = '';      // the text, when it is not the status word itself

  // a status that is not in the list gets the grey flag
  get colour(): string {
    return FLAGS[this.status] ?? 'grey';
  }

  // "REFUND_PENDING" is shown as "REFUND PENDING"
  get text(): string {
    if (this.label) {
      return this.label;
    }
    return this.status.split('_').join(' ');
  }
}
