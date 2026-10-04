// one row of the super admin's "Admins" page (GET /api/super-admin/admins)
export interface AdminAccount {
  userId: number;
  email: string;
  username: string;
  mobileNumber?: string;
  enabled: boolean;
  createdAt?: string;
}

export interface AddAdminRequest {
  email: string;
  username: string;
  initialPassword: string;
  mobileNumber?: string;
}
