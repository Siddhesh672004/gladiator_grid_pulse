// the chapters of the "How it works" page, in the order they are shown
// keep them short (one sentence, three rules) and the same as the real rules in the backend services
export interface Chapter {
  id: string;         // also picks the small drawn screen in the html
  number: string;
  role: string;       // Driver or Admin
  title: string;
  text: string;
  rules: string[];
  where: string;      // where to find it in the app, shown as the title bar of the drawn screen
}

export const CHAPTERS: Chapter[] = [
  {
    id: 'signup', number: '01', role: 'Driver', title: 'Sign up',
    text: 'An emailed 6-digit code, good for 10 minutes, confirms the address before the account exists.',
    rules: ['18 or older, license DL + 4 digits (DL1234)', 'Mobile: 10 digits, starts with 6, 7, 8 or 9',
      'Password: 8 to 64 characters, upper, lower, digit, special, no spaces'],
    where: 'Start page > Sign up'
  },
  {
    id: 'tracks', number: '02', role: 'Driver', title: 'Find a track',
    text: 'Only tracks with a session you can still book are listed.',
    rules: ['The price belongs to the session', 'Booking closes when the session starts', 'Search by name, place or category'],
    where: 'Driver > Tracks > View Tracks'
  },
  {
    id: 'book', number: '03', role: 'Driver', title: 'Book and pay',
    text: 'Pick a session and a car number, accept the terms, then pay. The booking is made after the payment, as Pending.',
    rules: ['The profile must be complete', 'Car number 1 to 999, free in that session', 'No two bookings at the same time'],
    where: 'Driver > Tracks > View Tracks > Register'
  },
  {
    id: 'approval', number: '04', role: 'Driver', title: 'Wait for the green flag',
    text: 'An admin approves or rejects the booking. An email tells you which.',
    rules: ['Seats count approved bookings only', 'Cancel while Pending: full refund', 'Cancel after approval: no refund'],
    where: 'Driver > Tracks > My Registrations'
  },
  {
    id: 'live', number: '05', role: 'Driver', title: 'Follow the race live',
    text: 'The leaderboard of a live session, refreshed every 4 seconds.',
    rules: ['Approved cars only', 'Sorted again on every lap', 'Times in India time (IST)'],
    where: 'Driver > Live Timing'
  },
  {
    id: 'feedback', number: '06', role: 'Driver', title: 'Rate the session',
    text: 'Stars and a short text after a completed session. AI can suggest clearer wording.',
    rules: ['Completed session, approved booking', 'One feedback per session', '1 to 5 stars, 10 to 1000 characters'],
    where: 'Driver > Feedback > Add Feedback'
  },
  {
    id: 'assistant', number: '07', role: 'Driver', title: 'Book by chat',
    text: 'The assistant answers from the FAQ and books a session in plain words.',
    rules: ['It asks for the car number', 'It shows a summary and the terms', 'Then it opens the payment'],
    where: 'Chat button, bottom right'
  },
  {
    id: 'dashboard', number: '08', role: 'Admin', title: 'See the whole operation',
    text: 'Users, bookings, live races and revenue on one screen.',
    rules: ['Revenue counts paid payments only', 'The charts show this year, month by month', 'The live monitor refreshes every 4 seconds'],
    where: 'Admin > Dashboard'
  },
  {
    id: 'schedule', number: '09', role: 'Admin', title: 'Add tracks and sessions',
    text: 'A track is the place. A session is a priced time slot on it.',
    rules: ['At least 30 minutes, inside the opening hours', 'Up to 12 months ahead, no overlap on a track',
      '1 to 5 pictures per track'],
    where: 'Admin > Tracks, Admin > Sessions'
  },
  {
    id: 'approve', number: '10', role: 'Admin', title: 'Approve or reject',
    text: 'Approving the last seat rejects and refunds everyone still waiting.',
    rules: ['A reject needs a reason, 5 to 500 characters', 'Only while the session is scheduled',
      'A refunded booking stays rejected'],
    where: 'Admin > Registrations'
  },
  {
    id: 'control', number: '11', role: 'Admin', title: 'Run the timing board',
    text: 'Start, +1 Lap, Retire, Finish. A competition runs through five phases.',
    rules: ['No lap before Start', 'Qualifying order becomes the race grid', 'The lap limit finishes a car'],
    where: 'Admin > Practice Control'
  },
  {
    id: 'money', number: '12', role: 'Admin', title: 'Payments and refunds',
    text: 'Every payment with its status. Refunds run in the background.',
    rules: ['A failed refund shows the reason', 'Retry refund sends it again', 'Payment rows are never deleted'],
    where: 'Admin > Payments'
  },
  {
    id: 'voices', number: '13', role: 'Admin', title: 'Feedback with AI labels',
    text: 'Each feedback is labelled Positive, Neutral or Negative.',
    rules: ['Drivers never see the label', 'AI down: saved as Not analysed', 'Summarise with AI reads the newest 50'],
    where: 'Admin > Feedbacks'
  },
  {
    id: 'people', number: '14', role: 'Admin', title: 'Users and admins',
    text: 'Switch a driver off or on. Only the super admin manages admins.',
    rules: ['A disabled account cannot log in', 'A new admin gets an invite email', 'Drivers are never deleted'],
    where: 'Admin > Users, Super admin > Admins'
  }
];
