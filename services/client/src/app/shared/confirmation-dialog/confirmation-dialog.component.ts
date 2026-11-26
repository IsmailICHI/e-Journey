import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { ConfirmationService } from '../../core/services/confirmation.service';

@Component({
  selector: 'app-confirmation-dialog',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div *ngIf="confirmation.current() as request"
      class="fixed inset-0 z-[100] flex items-center justify-center bg-black/70 p-4 backdrop-blur-sm"
      (click)="confirmation.cancel()">
      <section role="alertdialog" aria-modal="true" aria-labelledby="confirmation-title"
        aria-describedby="confirmation-message"
        class="w-full max-w-md border border-stone-200 bg-[#fbfaf7] shadow-2xl"
        (click)="$event.stopPropagation()">
        <div class="border-b border-stone-200 px-6 py-5">
          <p class="text-[10px] font-semibold tracking-[0.2em] text-[#8b6c38]">PLEASE CONFIRM</p>
          <h2 id="confirmation-title" class="mt-2 text-xl font-medium text-stone-900">{{ request.title }}</h2>
        </div>
        <p id="confirmation-message" class="px-6 py-5 text-sm leading-6 text-stone-600">{{ request.message }}</p>
        <div class="flex justify-end gap-3 border-t border-stone-200 bg-stone-50 px-6 py-4">
          <button type="button" (click)="confirmation.cancel()"
            class="min-h-10 border border-stone-300 px-4 text-xs font-semibold tracking-[0.08em] text-stone-700 transition hover:bg-white">
            {{ request.cancelLabel ?? 'CANCEL' }}
          </button>
          <button type="button" (click)="confirmation.accept()"
            [ngClass]="request.tone === 'danger'
              ? 'border border-red-800 bg-red-800 text-white hover:bg-red-700'
              : 'border border-stone-900 bg-stone-900 text-white hover:bg-stone-700'"
            class="min-h-10 px-4 text-xs font-semibold tracking-[0.08em] transition">
            {{ request.confirmLabel }}
          </button>
        </div>
      </section>
    </div>
  `
})
export class ConfirmationDialogComponent {
  readonly confirmation = inject(ConfirmationService);
}
