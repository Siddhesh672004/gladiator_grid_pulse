// the sentence our backend sent with an error (ErrorResponse.message), or the page's own text when there is none
export function errorMessage(err: any, fallback: string): string {
  return err?.error?.message || fallback;
}
