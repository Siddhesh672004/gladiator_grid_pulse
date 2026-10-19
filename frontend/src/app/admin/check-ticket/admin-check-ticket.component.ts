import { Component } from '@angular/core';
import { TicketCheck } from '../models/ticket.model';
import { TicketService } from '../services/ticket.service';
import { errorMessage } from '../../shared/utils/error-message';

// Check Ticket (/admin/check-ticket): the code under the QR of an e-ticket -> valid or not + the booking -> TicketService
@Component({
  selector: 'app-admincheckticket',
  templateUrl: './admin-check-ticket.component.html'
})
export class AdminCheckTicketComponent {

  code: string = '';
  result: TicketCheck | null = null;
  isChecking: boolean = false;
  errorMessage: string = '';

  constructor(private ticketService: TicketService) { }

  check(): void {
    const code = this.code.trim();
    this.errorMessage = '';
    this.result = null;
    if (!code) {
      this.errorMessage = 'Please type or scan the ticket code';
      return;
    }
    this.isChecking = true;
    this.ticketService.verify(code).subscribe({
      next: (result) => {
        this.isChecking = false;
        this.result = result;
      },
      error: (err) => {
        this.isChecking = false;
        this.errorMessage = errorMessage(err, 'Unable to check the ticket');
      }
    });
  }
}
