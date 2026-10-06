// same fields as RaceTrack in spring, times come as "07:00:00"
// no price: every session has its own price
export interface RaceTrack {
  trackId?: number;
  trackName: string;
  description: string;
  location: string;
  trackCategory: string;
  isIndoor: boolean;
  openTime: string;
  closeTime: string;
  facilities: string;
  active?: boolean;
  imageUrls: string[];   // 1 - 5 links, the first one is the cover
  upcomingSessions?: number;
  fromPrice?: number | null;
}
