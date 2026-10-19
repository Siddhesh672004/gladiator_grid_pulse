import { Component, OnInit } from '@angular/core';
import { AdminUser } from '../models/admin-user.model';
import { AdminUserService } from '../services/admin-user.service';
import { FieldOption, toOptions } from '../../shared/utils/field-option';
import { byKey } from '../../shared/utils/track-by';
import { errorMessage } from '../../shared/utils/error-message';

// Manage Users (/admin/manage-users): driver accounts, search + Active / Inactive filter, Activate / Deactivate -> AdminUserService
// paged by the database (10 rows per request); a deactivated driver cannot log in, their bookings stay
@Component({
  selector: 'app-adminmanageusers',
  templateUrl: './admin-manage-users.component.html'
})
export class AdminManageUsersComponent implements OnInit {

  users: AdminUser[] = [];
  total: number = 0;
  page: number = 1;                // the page on screen, starts at 1 (the backend counts from 0)
  pageSize: number = 10;
  searchText: string = '';
  statusFilter: string = 'All';
  statusOptions: FieldOption[] = toOptions(['Active', 'Inactive']);
  isLoading: boolean = true;
  busyId: number | null = null;
  errorMessage: string = '';

  readonly byUser = byKey<AdminUser>('userId');

  constructor(private adminUserService: AdminUserService) { }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.errorMessage = '';
    this.isLoading = true;
    this.adminUserService.getUsers(this.searchText.trim(), this.statusFilter, this.page - 1, this.pageSize).subscribe({
      next: (result) => {
        this.users = result.content;
        this.total = result.totalElements;
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = errorMessage(err, 'Unable to load the users');
      }
    });
  }

  search(): void {
    this.page = 1;
    this.load();
  }

  goToPage(page: number): void {
    this.page = page;
    this.load();
  }

  toggle(user: AdminUser): void {
    this.errorMessage = '';
    this.busyId = user.userId;
    this.adminUserService.setEnabled(user.userId, !user.enabled).subscribe({
      next: (updated) => {
        this.busyId = null;
        // put the answer of the backend in place of the old row
        const index = this.users.findIndex(row => row.userId === updated.userId);
        if (index >= 0) {
          this.users[index] = updated;
        }
        // with a status filter on, the changed driver no longer belongs in this list
        if (this.statusFilter !== 'All') {
          this.load();
        }
      },
      error: (err) => {
        this.busyId = null;
        this.errorMessage = errorMessage(err, 'Unable to change the user');
      }
    });
  }
}
