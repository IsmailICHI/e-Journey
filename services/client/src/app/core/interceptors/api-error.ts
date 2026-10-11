import { HttpErrorResponse } from '@angular/common/http';

export function getApiErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const body = error.error as { message?: unknown; error?: unknown } | null;
    if (typeof body?.message === 'string' && body.message.trim()) {
      return body.message;
    }
    if (typeof body?.error === 'string' && body.error.trim()) {
      return body.error;
    }
    if (error.status === 0) {
      return 'Could not reach the API gateway. Check that the backend services are running.';
    }
    return `The request failed (${error.status}). Please try again.`;
  }
  return error instanceof Error ? error.message : 'An unexpected error occurred. Please try again.';
}
