// one driver account on the Manage Users page (GET /api/admin/users)
export interface AdminUser {
  userId: number;
  username: string;
  email: string;
  mobileNumber: string;
  enabled: boolean;
  profileComplete: boolean;
  createdAt: string | null;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  page: number;                // starts at 0
  size: number;
}
