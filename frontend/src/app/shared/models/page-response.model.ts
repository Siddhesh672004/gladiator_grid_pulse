// one page of a server-side list (the /page endpoints), same names as the java record
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  page: number;                // starts at 0
  size: number;
}
