// an account as the backend sends it, never with a password
export interface User {
  userId?: number;
  email: string;
  username: string;
  mobileNumber: string;
  userRole: string;
}

// signup form, no role: signing up always makes a driver
// sent to /api/register/send-code without otp, then to /api/register with the emailed otp
export interface SignupRequest {
  email: string;
  password: string;
  username: string;
  mobileNumber: string;
  dateOfBirth: string;            // "yyyy-MM-dd"
  driverLicenseNumber: string;
  otp?: string;
}
