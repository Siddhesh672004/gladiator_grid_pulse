// used by every list page and by the page buttons under a table

export function pageCount(total: number, pageSize: number): number {
  return Math.max(1, Math.ceil(total / pageSize));
}

// the rows of one page. a list that got shorter (search, delete) stays on its last page
export function pageOf<T>(rows: T[], page: number, pageSize: number): T[] {
  const shownPage = Math.min(page, pageCount(rows.length, pageSize));
  const start = (shownPage - 1) * pageSize;
  return rows.slice(start, start + pageSize);
}
