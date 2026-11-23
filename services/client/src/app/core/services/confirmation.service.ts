import { Injectable, signal } from '@angular/core';

export interface ConfirmationRequest {
  title: string;
  message: string;
  confirmLabel: string;
  cancelLabel?: string;
  tone?: 'danger' | 'default';
}

@Injectable({ providedIn: 'root' })
export class ConfirmationService {
  readonly current = signal<ConfirmationRequest | null>(null);
  private resolveConfirmation: ((confirmed: boolean) => void) | null = null;

  confirm(request: ConfirmationRequest): Promise<boolean> {
    if (this.current()) {
      return Promise.resolve(false);
    }

    this.current.set(request);
    return new Promise((resolve) => {
      this.resolveConfirmation = resolve;
    });
  }

  accept(): void {
    this.finish(true);
  }

  cancel(): void {
    this.finish(false);
  }

  private finish(confirmed: boolean): void {
    this.current.set(null);
    const resolve = this.resolveConfirmation;
    this.resolveConfirmation = null;
    resolve?.(confirmed);
  }
}
