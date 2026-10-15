// every backend path in one place
export const API_ENDPOINTS = {
  // sign up: SEND_CODE emails the code, REGISTER (with that code) makes the account
  REGISTER_SEND_CODE: '/api/register/send-code',
  REGISTER: '/api/register',
  LOGIN: '/api/login',
  ADMIN_LOGIN: '/api/admin/login',
  PASSWORD_FORGOT: '/api/password/forgot',
  PASSWORD_RESET: '/api/password/reset',
  PASSWORD_SET_INITIAL: '/api/password/set-initial',
  PROFILE: '/api/profile',
  IMAGE_UPLOAD: (kind: string) => `/api/images/${kind}`,
  SUPER_ADMIN_ADMINS: '/api/super-admin/admins',
  SUPER_ADMIN_RESEND_INVITE: (userId: number) => `/api/super-admin/admins/${userId}/resend-invite`,
  RACE_TRACK: '/api/race-track',
  RACE_TRACK_ACTIVATE: (trackId: number) => `/api/race-track/${trackId}/activate`,
  RACE_TRACK_DEACTIVATE: (trackId: number) => `/api/race-track/${trackId}/deactivate`,
  SESSIONS: '/api/sessions',
  SESSIONS_PAGE: '/api/sessions/page',
  SESSIONS_BY_TRACK: (trackId: number) => `/api/sessions/track/${trackId}`,
  SESSION_CANCEL: (sessionId: number) => `/api/sessions/${sessionId}/cancel`,
  COMPETITIONS: '/api/competitions',
  COMPETITION_CANCEL: (competitionId: number) => `/api/competitions/${competitionId}/cancel`,
  COMPETITION_RULE: (trackId: number) => `/api/competitions/rule/${trackId}`,
  // step = 'qualifying/start', 'qualifying/finish', 'race/start' or 'race/finish'
  COMPETITION_STEP: (competitionId: number, step: string) => `/api/competition-control/${competitionId}/${step}`,
  COMPETITION_LAP: (raceBookingId: number) => `/api/competition-control/lap/${raceBookingId}`,
  COMPETITION_RETIRE: (raceBookingId: number) => `/api/competition-control/retire/${raceBookingId}`,
  RACE_BOOKING: '/api/race-booking',
  RACE_BOOKING_PAGE: '/api/race-booking/page',
  RACE_BOOKING_TICKET: (raceBookingId: number) => `/api/race-booking/${raceBookingId}/ticket`,
  TICKET_VERIFY: (code: string) => `/api/tickets/verify/${encodeURIComponent(code)}`,
  RACE_BOOKING_BY_USER: '/api/race-booking/user',
  RACE_BOOKING_CAR_CATEGORIES: '/api/race-booking/car-categories',
  PAYMENT_ORDER: '/api/payments/order',
  PAYMENT_CONFIRM: '/api/payments/confirm',
  PAYMENTS: '/api/payments',
  PAYMENTS_PAGE: '/api/payments/page',
  PAYMENT_SUMMARY: '/api/payments/summary',
  PAYMENT_AUDIT: '/api/payments/audit',
  PAYMENT_RETRY_REFUND: (paymentId: number) => `/api/payments/${paymentId}/retry-refund`,
  ADMIN_DASHBOARD: '/api/admin/dashboard',
  ADMIN_LIVE_RACES: '/api/admin/dashboard/live-races',
  ADMIN_USERS: '/api/admin/users',
  ADMIN_USER_STATUS: (userId: number) => `/api/admin/users/${userId}/status`,
  RACE_CONTROL_SESSION: (sessionId: number) => `/api/race-control/session/${sessionId}`,
  // server-sent events: the board pushed after every change (LiveBoardService)
  LIVE_SESSION: (sessionId: number) => `/api/live/session/${sessionId}`,
  RACE_CONTROL_START: (raceBookingId: number) => `/api/race-control/${raceBookingId}/start`,
  RACE_CONTROL_LAP: (raceBookingId: number) => `/api/race-control/${raceBookingId}/lap`,
  RACE_CONTROL_RETIRE: (raceBookingId: number) => `/api/race-control/${raceBookingId}/retire`,
  RACE_CONTROL_RESET: (sessionId: number) => `/api/race-control/session/${sessionId}/reset`,
  RACE_CONTROL_FINISH: (sessionId: number) => `/api/race-control/session/${sessionId}/finish`,
  FEEDBACK: '/api/feedback',
  FEEDBACK_PAGE: '/api/feedback/page',
  FEEDBACK_COUNTS: '/api/feedback/counts',
  FEEDBACK_BY_USER: '/api/feedback/user',
  FEEDBACK_SUGGESTIONS: '/api/feedback/suggestions',
  FEEDBACK_SUMMARY: '/api/feedback/summary',
  FEEDBACK_ANALYSE: (feedbackId: number) => `/api/feedback/${feedbackId}/analyse`,
  CHAT: '/api/chat',
  STATS_ME: '/api/stats/me',
  STATS_LEADERBOARD: '/api/stats/leaderboard',
  CHAT_MEMORY: '/api/chat/memory'
};

// no Bearer token for these (same list as permitAll in SecurityConfig), matched on the path only
export const PUBLIC_ENDPOINTS: { method: string; pattern: RegExp }[] = [
  { method: 'POST', pattern: /\/api\/(register|register\/send-code|login|admin\/login|password\/forgot|password\/reset|password\/set-initial)$/ },
  { method: 'GET', pattern: /\/api\/race-track(\/\d+)?$/ },
  { method: 'GET', pattern: /\/api\/race-control\/session\/\d+$/ },
  { method: 'GET', pattern: /\/api\/live\/session\/\d+$/ },
  { method: 'GET', pattern: /\/api\/sessions\/(track\/)?\d+$/ },
  { method: 'GET', pattern: /\/api\/competitions(\/\d+)?$/ },
  { method: 'GET', pattern: /\/api\/stats\/leaderboard$/ },
  // POST /api/chat is not listed: a logged-in driver's token must go along, the assistant books for them
  { method: '*', pattern: /\/api\/chat\/.+$/ },
  { method: 'GET', pattern: /\/api\/faqs$/ }
];

export const STORAGE_KEYS = {
  TOKEN: 'token',
  USER_ROLE: 'userRole',
  USERNAME: 'username',
  USER_ID: 'userId'
};

export const CHAT_SESSION_KEY = 'gridpulseChatSessionId';

// SuperAdmin = the predefined owner: everything an Admin can do + managing admins
export const ROLES = {
  SUPER_ADMIN: 'SuperAdmin',
  ADMIN: 'Admin',
  USER: 'User'
};

export const ADMIN_ROLES = [ROLES.ADMIN, ROLES.SUPER_ADMIN];

// allowed values, same lists the backend checks
export const TRACK_CATEGORIES = ['Circuit', 'Karting', 'Rally', 'Drag Strip'];
export const CAR_CATEGORIES = ['GT3', 'Formula', 'Karting', 'Rally', 'Stock'];
export const BOOKING_STATUSES = ['Pending', 'Approved', 'Rejected'];
export const SENTIMENTS = ['POSITIVE', 'NEUTRAL', 'NEGATIVE', 'NOT_ANALYSED'];

// same words as the backend enums
export const SESSION_TYPE = {
  PRACTICE: 'PRACTICE',
  COMPETITION: 'COMPETITION'
};

export const COMPETITION_PHASE = {
  QUALIFYING: 'QUALIFYING',
  RACING: 'RACING'
};

export const SESSION_STATUS = {
  SCHEDULED: 'SCHEDULED',
  LIVE: 'LIVE',
  COMPLETED: 'COMPLETED',
  CANCELLED: 'CANCELLED'
};

export const COMPETITION_STATUS = {
  UPCOMING: 'UPCOMING',
  QUALIFYING: 'QUALIFYING',
  QUALIFYING_COMPLETED: 'QUALIFYING_COMPLETED',
  RACING: 'RACING',
  COMPLETED: 'COMPLETED',
  CANCELLED: 'CANCELLED'
};

export const BOOKING_STATUS = {
  PENDING: 'Pending',
  APPROVED: 'Approved',
  REJECTED: 'Rejected'
};

export const BOOKING_TERMS = 'You pay when you book. If GridPulse rejects your booking, or you cancel it while it is '
  + 'still Pending, the full amount is refunded. You may also cancel an approved booking, but then the amount is not refunded.';

export const LIVE_REFRESH_MS = 4000;

// the admin lists ask the backend once the typing stops
export const SEARCH_DEBOUNCE_MS = 300;

// the backend sends moments in UTC, the boards show them in this time zone
export const RACE_TIME_ZONE = 'Asia/Kolkata';
export const RACE_TIME_ZONE_LABEL = 'IST';

export const RACE_STATES = {
  NOT_STARTED: 'NOT_STARTED',
  RACING: 'RACING',
  RETIRED: 'RETIRED',
  FINISHED: 'FINISHED'
};
