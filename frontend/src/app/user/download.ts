import { errorMessage } from '../shared/utils/error-message';

// files from the api (the e-ticket pdf): save them, read their name, read an error that came as a file

// the browser saves the file under this name
export function saveFile(data: Blob, filename: string): void {
  const url = URL.createObjectURL(data);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

// 'attachment; filename="gridpulse-ticket-7.pdf"' -> gridpulse-ticket-7.pdf
export function fileNameOf(contentDisposition: string | null, fallback: string): string {
  const match = /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i.exec(contentDisposition ?? '');
  return match ? decodeURIComponent(match[1]) : fallback;
}

// a download that failed still sends our json error, only as a blob
export async function blobErrorMessage(err: any, fallback: string): Promise<string> {
  if (err?.error instanceof Blob) {
    try {
      return JSON.parse(await err.error.text())?.message || fallback;
    } catch {
      return fallback;
    }
  }
  return errorMessage(err, fallback);
}
