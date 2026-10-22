import { HttpErrorResponse } from '@angular/common/http';

export interface ApiErrorPayload {
  timestamp?: string;
  status?: number;
  error?: string;
  message?: string;
  errorMessage?: string;
  path?: string;
  validationErrors?: Record<string, string>;
  traceId?: string;
}

export interface ParsedApiError {
  status: number;
  title: string;
  message: string;
  path?: string;
  validationErrors?: Record<string, string>;
  hasValidationErrors: boolean;
}

export function parseApiError(error: unknown): ParsedApiError {
  if (error instanceof HttpErrorResponse) {
    const body = error.error as ApiErrorPayload | null;
    const status = error.status || 0;
    const title = typeof body?.error === 'string' && body.error.trim()
      ? body.error
      : status === 0
      ? 'Connection Failed'
      : `HTTP ${status} Error`;

    let validationErrors: Record<string, string> | undefined = undefined;
    if (body?.validationErrors && typeof body.validationErrors === 'object' && !Array.isArray(body.validationErrors)) {
      validationErrors = body.validationErrors;
    }

    let message = '';
    if (typeof body?.message === 'string' && body.message.trim()) {
      message = body.message;
    } else if (typeof body?.errorMessage === 'string' && body.errorMessage.trim()) {
      message = body.errorMessage;
    } else if (typeof body?.error === 'string' && body.error.trim()) {
      message = body.error;
    } else if (status === 0) {
      message = 'Could not reach the server or API gateway. Please check that backend services are running.';
    } else {
      message = `The request failed (${status}). Please try again.`;
    }

    const hasValidationErrors = Boolean(validationErrors && Object.keys(validationErrors).length > 0);
    if (hasValidationErrors && message === 'Validation failed') {
      const fieldCount = Object.keys(validationErrors!).length;
      message = `Validation failed for ${fieldCount} field${fieldCount > 1 ? 's' : ''}.`;
    }

    return {
      status,
      title,
      message,
      path: typeof body?.path === 'string' ? body.path : undefined,
      validationErrors,
      hasValidationErrors
    };
  }

  const rawMessage = error instanceof Error ? error.message : 'An unexpected error occurred. Please try again.';
  return {
    status: 500,
    title: 'Unexpected Error',
    message: rawMessage,
    hasValidationErrors: false
  };
}

export function getApiErrorMessage(error: unknown): string {
  const parsed = parseApiError(error);
  if (parsed.hasValidationErrors && parsed.validationErrors) {
    const details = Object.entries(parsed.validationErrors)
      .map(([field, msg]) => `${field}: ${msg}`)
      .join('; ');
    return `${parsed.message} [${details}]`;
  }
  return parsed.message;
}
