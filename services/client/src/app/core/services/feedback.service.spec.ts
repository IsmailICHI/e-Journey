import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { FeedbackService } from './feedback.service';

describe('FeedbackService', () => {
  let service: FeedbackService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(FeedbackService);
  });

  it('manages modal dialog feedback with field-level validation errors', () => {
    service.showError('Invalid input', 'Validation Failure', { title: 'Required' }, 400);

    const dialog = service.activeDialog();
    expect(dialog).not.toBeNull();
    expect(dialog?.type).toBe('error');
    expect(dialog?.title).toBe('Validation Failure');
    expect(dialog?.status).toBe(400);
    expect(dialog?.validationErrors?.['title']).toBe('Required');

    service.closeDialog();
    expect(service.activeDialog()).toBeNull();
  });

  it('automatically parses HttpErrorResponse and formats 400 validation dialog', () => {
    const httpError = new HttpErrorResponse({
      error: {
        status: 400,
        error: 'Bad Request',
        message: 'Validation failed for 1 field(s)',
        validationErrors: {
          city: 'City is required'
        }
      },
      status: 400
    });

    service.showFromApiError(httpError);

    const dialog = service.activeDialog();
    expect(dialog).not.toBeNull();
    expect(dialog?.status).toBe(400);
    expect(dialog?.title).toContain('400 Bad Request');
    expect(dialog?.validationErrors?.['city']).toBe('City is required');
  });

  it('manages toast notifications and activity log history', () => {
    service.showSuccess('Operation completed', 'Saved');

    expect(service.toast()?.title).toBe('Saved');
    expect(service.history().length).toBeGreaterThan(0);
    expect(service.history()[0].title).toBe('Saved');

    service.openLog();
    expect(service.isLogOpen()).toBeTrue();

    service.closeLog();
    expect(service.isLogOpen()).toBeFalse();

    service.clearHistory();
    expect(service.history().length).toBe(0);
  });
});
