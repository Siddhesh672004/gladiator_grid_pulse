// one row of the super admin's "Admins" page (GET /api/super-admin/admins)
export interface AdminAccount {
  userId: number;
  email: string;
  username: string;
  mobileNumber?: string;
  enabled: boolean;
  createdAt?: string;
}

// no password: the new admin chooses it through the emailed link
export interface AddAdminRequest {
  email: string;
  username: string;
  mobileNumber?: string;
}
