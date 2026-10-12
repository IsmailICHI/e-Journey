import { HttpErrorResponse } from '@angular/common/http';
import { getApiErrorMessage, parseApiError } from './api-error';

describe('api-error helpers', () => {
  it('parses structured 400 validation error responses with field-level details', () => {
    const errorResponse = new HttpErrorResponse({
      error: {
        timestamp: '2026-10-12T11:00:00Z',
        status: 400,
        error: 'Bad Request',
        message: 'Validation failed for 2 field(s)',
        path: '/api/v1/admin/travels',
        validationErrors: {
          title: 'Title must be between 3 and 120 characters',
          duration: 'Duration must be greater than 0'
        }
      },
      status: 400,
      statusText: 'Bad Request'
    });

    const parsed = parseApiError(errorResponse);
    expect(parsed.status).toBe(400);
    expect(parsed.hasValidationErrors).toBeTrue();
    expect(parsed.validationErrors?.['title']).toBe('Title must be between 3 and 120 characters');
    expect(parsed.validationErrors?.['duration']).toBe('Duration must be greater than 0');

    const formattedMessage = getApiErrorMessage(errorResponse);
    expect(formattedMessage).toContain('title: Title must be between 3 and 120 characters');
    expect(formattedMessage).toContain('duration: Duration must be greater than 0');
  });

  it('handles standard message and error fields when validationErrors is absent', () => {
    const errorResponse = new HttpErrorResponse({
      error: {
        status: 404,
        error: 'Not Found',
        message: 'Travel with ID 42 not found'
      },
      status: 404,
      statusText: 'Not Found'
    });

    const parsed = parseApiError(errorResponse);
    expect(parsed.status).toBe(404);
    expect(parsed.title).toBe('Not Found');
    expect(parsed.message).toBe('Travel with ID 42 not found');
    expect(parsed.hasValidationErrors).toBeFalse();
    expect(getApiErrorMessage(errorResponse)).toBe('Travel with ID 42 not found');
  });

  it('handles network failure (status 0)', () => {
    const errorResponse = new HttpErrorResponse({
      status: 0,
      statusText: 'Unknown Error'
    });

    const parsed = parseApiError(errorResponse);
    expect(parsed.status).toBe(0);
    expect(parsed.title).toBe('Connection Failed');
    expect(parsed.message).toContain('Could not reach');
  });
});
