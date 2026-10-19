import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { AddAdminRequest, AdminAccount } from '../../models/admin.model';
import { SuperAdminService } from '../../services/super-admin.service';
import { emailRules, optionalMobileRules, usernameRules } from '../../validation';
import { pageOf } from '../../shared/paging';
import { byKey } from '../../shared/track-by';

// super admin only (/admin/manage-admins): add (the admin gets a set-password link), resend the link, switch on / off, delete -> SuperAdminService
@Component({
  selector: 'app-superadminmanageadmins',
  templateUrl: './superadminmanageadmins.component.html'
})
export class SuperadminmanageadminsComponent implements OnInit {

  adminForm: FormGroup;
  admins: AdminAccount[] = [];
  page: number = 1;
  pageSize: number = 10;
  isLoading: boolean = true;
  isSaving: boolean = false;
  busyId: number | null = null;
  errorMessage: string = '';
  successMessage: string = '';
  adminToDelete: AdminAccount | null = null;

  readonly byAdmin = byKey<AdminAccount>('userId');

  constructor(private fb: FormBuilder, private superAdminService: SuperAdminService) {
    this.adminForm = this.fb.group({
      username: ['', usernameRules],
      email: ['', emailRules],
      mobileNumber: ['', optionalMobileRules]
    });
  }

  ngOnInit(): void {
    this.loadAdmins();
  }

  loadAdmins(): void {
    this.superAdminService.getAdmins().subscribe({
      next: (admins) => {
        this.admins = admins;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.errorMessage = 'Unable to load admins';
      }
    });
  }

  get pagedAdmins(): AdminAccount[] {
    return pageOf(this.admins, this.page, this.pageSize);
  }

  addAdmin(): void {
    this.errorMessage = '';
    this.successMessage = '';
    if (this.adminForm.invalid) {
      this.adminForm.markAllAsTouched();
      return;
    }
    const value = this.adminForm.value;
    const admin: AddAdminRequest = {
      username: value.username.trim(),
      email: value.email.trim(),
      mobileNumber: value.mobileNumber?.trim() || undefined
    };
    this.isSaving = true;
    this.superAdminService.addAdmin(admin).subscribe({
      next: (saved) => {
        this.isSaving = false;
        this.successMessage = `Admin added - a link to choose the password was emailed to ${saved.email}`;
        this.adminForm.reset({ username: '', email: '', mobileNumber: '' });
        this.page = 1;
        this.loadAdmins();
      },
      error: (err) => {
        this.isSaving = false;
        this.errorMessage = err.error?.message || 'Unable to add the admin';
      }
    });
  }

  resendInvite(admin: AdminAccount): void {
    this.errorMessage = '';
    this.successMessage = '';
    this.busyId = admin.userId;
    this.superAdminService.resendInvite(admin.userId).subscribe({
      next: (res) => {
        this.busyId = null;
        this.successMessage = res.message;
      },
      error: (err) => {
        this.busyId = null;
        this.errorMessage = err.error?.message || 'Unable to send the invite';
      }
    });
  }

  deleteAdmin(): void {
    const admin = this.adminToDelete;
    this.adminToDelete = null;
    if (!admin) {
      return;
    }
    this.errorMessage = '';
    this.successMessage = '';
    this.busyId = admin.userId;
    this.superAdminService.deleteAdmin(admin.userId).subscribe({
      next: () => {
        this.busyId = null;
        this.admins = this.admins.filter(a => a.userId !== admin.userId);
        this.successMessage = `Admin ${admin.email} was deleted`;
      },
      error: (err) => {
        this.busyId = null;
        this.errorMessage = err.error?.message || 'Unable to delete the admin';
      }
    });
  }

  toggle(admin: AdminAccount): void {
    this.errorMessage = '';
    this.successMessage = '';
    this.busyId = admin.userId;
    this.superAdminService.setEnabled(admin.userId, !admin.enabled).subscribe({
      next: (updated) => {
        this.busyId = null;
        // put the answer of the backend in place of the old row
        const index = this.admins.findIndex(row => row.userId === updated.userId);
        if (index >= 0) {
          this.admins[index] = updated;
        }
      },
      error: (err) => {
        this.busyId = null;
        this.errorMessage = err.error?.message || 'Unable to change the admin';
      }
    });
  }
}
