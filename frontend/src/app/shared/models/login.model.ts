// what both login forms send (/api/login for drivers, /api/admin/login for admins)
export interface Login {
  email: string;
  password: string;
}

// sign up step 1 answer: the code is on its way by email
export interface OtpChallenge {
  email: string;
  message: string;
  expiresInSeconds: number;
}

export interface LoginResponse {
  token: string;
  username: string;
  userRole: string;
  userId: number;
}

export interface MessageResponse {
  message: string;
}
