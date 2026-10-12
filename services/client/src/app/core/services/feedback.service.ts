import { Injectable, signal } from '@angular/core';
import { parseApiError } from '../interceptors/api-error';

export type FeedbackType = 'error' | 'success' | 'warning' | 'info';

export interface FeedbackItem {
  id: string;
  type: FeedbackType;
  title: string;
  message: string;
  status?: number;
  path?: string;
  validationErrors?: Record<string, string>;
  timestamp: Date;
}

@Injectable({
  providedIn: 'root'
})
export class FeedbackService {
  readonly activeDialog = signal<FeedbackItem | null>(null);
  readonly toast = signal<FeedbackItem | null>(null);
  readonly history = signal<FeedbackItem[]>([]);
  readonly isLogOpen = signal<boolean>(false);

  private toastTimeout: ReturnType<typeof setTimeout> | null = null;

  showError(
    message: string,
    title = 'Request Error',
    validationErrors?: Record<string, string>,
    status?: number,
    path?: string
  ): FeedbackItem {
    const item: FeedbackItem = {
      id: this.generateId(),
      type: 'error',
      title,
      message,
      status,
      path,
      validationErrors,
      timestamp: new Date()
    };
    this.record(item);
    this.activeDialog.set(item);
    return item;
  }

  showFromApiError(error: unknown, fallbackTitle = 'Action Failed'): FeedbackItem {
    const parsed = parseApiError(error);
    const title = parsed.status === 400 && parsed.hasValidationErrors
      ? 'Validation Failed (400 Bad Request)'
      : parsed.status === 400
      ? 'Invalid Request (400 Bad Request)'
      : parsed.title || fallbackTitle;

    return this.showError(
      parsed.message,
      title,
      parsed.validationErrors,
      parsed.status,
      parsed.path
    );
  }

  showSuccess(message: string, title = 'Success', asDialog = false): FeedbackItem {
    const item: FeedbackItem = {
      id: this.generateId(),
      type: 'success',
      title,
      message,
      timestamp: new Date()
    };
    this.record(item);

    if (asDialog) {
      this.activeDialog.set(item);
    } else {
      this.displayToast(item);
    }
    return item;
  }

  showWarning(message: string, title = 'Attention'): FeedbackItem {
    const item: FeedbackItem = {
      id: this.generateId(),
      type: 'warning',
      title,
      message,
      timestamp: new Date()
    };
    this.record(item);
    this.activeDialog.set(item);
    return item;
  }

  showInfo(message: string, title = 'Information'): FeedbackItem {
    const item: FeedbackItem = {
      id: this.generateId(),
      type: 'info',
      title,
      message,
      timestamp: new Date()
    };
    this.record(item);
    this.displayToast(item);
    return item;
  }

  closeDialog(): void {
    this.activeDialog.set(null);
  }

  dismissToast(): void {
    if (this.toastTimeout) {
      clearTimeout(this.toastTimeout);
      this.toastTimeout = null;
    }
    this.toast.set(null);
  }

  openLog(): void {
    this.isLogOpen.set(true);
  }

  closeLog(): void {
    this.isLogOpen.set(false);
  }

  clearHistory(): void {
    this.history.set([]);
  }

  private displayToast(item: FeedbackItem): void {
    this.dismissToast();
    this.toast.set(item);
    this.toastTimeout = setTimeout(() => {
      this.toast.set(null);
      this.toastTimeout = null;
    }, 4500);
  }

  private record(item: FeedbackItem): void {
    this.history.update((prev) => [item, ...prev].slice(0, 50));
  }

  private generateId(): string {
    return Math.random().toString(36).substring(2, 9);
  }
}
