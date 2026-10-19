// the profile of the logged-in user (ProfileResponse)
export interface Profile {
  userId: number;
  email: string;
  username: string;
  mobileNumber: string;
  userRole: string;
  dateOfBirth: string | null;           // "yyyy-MM-dd"
  driverLicenseNumber: string | null;
  profilePicture: string | null;
  profileComplete: boolean;
}

// PUT /api/profile. admins send only the picture
export interface ProfileRequest {
  dateOfBirth: string | null;
  driverLicenseNumber: string | null;
  profilePicture: string | null;
}
