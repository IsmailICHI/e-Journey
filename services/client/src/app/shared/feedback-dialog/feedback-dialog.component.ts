import { CommonModule } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { FeedbackItem, FeedbackService, FeedbackType } from '../../core/services/feedback.service';

@Component({
  selector: 'app-feedback-dialog',
  standalone: true,
  imports: [CommonModule],
  template: `
    <!-- Top-Right Toast Notifications Container -->
    <aside
      *ngIf="toast() as item"
      aria-live="polite"
      class="fixed right-4 top-20 z-[90] w-full max-w-sm transition-all duration-300 ease-out"
    >
      <div
        class="flex items-start gap-3 rounded-xl border p-4 shadow-2xl backdrop-blur-xl"
        [ngClass]="getToastClasses(item.type)"
      >
        <div class="mt-0.5 shrink-0">
          <span
            class="flex h-5 w-5 items-center justify-center rounded-full text-xs font-bold"
            [ngClass]="getIconBadgeClasses(item.type)"
          >
            {{ getIconSymbol(item.type) }}
          </span>
        </div>
        <div class="min-w-0 flex-1">
          <h4 class="text-xs font-semibold tracking-wide text-white">{{ item.title }}</h4>
          <p class="mt-1 text-xs leading-5 text-stone-300">{{ item.message }}</p>
        </div>
        <button
          type="button"
          (click)="feedback.dismissToast()"
          class="shrink-0 text-stone-400 hover:text-white"
          aria-label="Dismiss toast notification"
        >
          ×
        </button>
      </div>
    </aside>

    <!-- Modal Dialog for High-Priority Feedback / Validation Errors -->
    <div
      *ngIf="dialog() as item"
      role="dialog"
      aria-modal="true"
      [attr.aria-labelledby]="'dialog-title-' + item.id"
      class="fixed inset-0 z-[100] flex items-center justify-center overflow-y-auto bg-black/80 p-4 backdrop-blur-md"
      (click)="feedback.closeDialog()"
    >
      <div
        class="my-auto w-full max-w-lg overflow-hidden rounded-2xl border bg-[#11100f] text-stone-100 shadow-[0_32px_120px_rgba(0,0,0,0.85)] transition-all"
        [ngClass]="getDialogBorderClasses(item.type)"
        (click)="$event.stopPropagation()"
      >
        <!-- Modal Header -->
        <div class="border-b border-white/10 px-6 py-5 sm:px-7">
          <div class="flex items-start justify-between gap-4">
            <div class="flex items-center gap-3">
              <span
                class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-sm font-bold"
                [ngClass]="getIconBadgeClasses(item.type)"
              >
                {{ getIconSymbol(item.type) }}
              </span>
              <div>
                <div class="flex items-center gap-2">
                  <span
                    class="rounded px-2 py-0.5 font-mono text-[10px] font-semibold tracking-wider uppercase"
                    [ngClass]="getStatusPillClasses(item)"
                  >
                    {{ getStatusText(item) }}
                  </span>
                  <span *ngIf="item.path" class="hidden font-mono text-[10px] text-stone-500 sm:inline truncate max-w-[200px]">
                    {{ item.path }}
                  </span>
                </div>
                <h3 [id]="'dialog-title-' + item.id" class="mt-1 font-sans-clean text-xl font-medium tracking-tight text-white">
                  {{ item.title }}
                </h3>
              </div>
            </div>
            <button
              type="button"
              (click)="feedback.closeDialog()"
              aria-label="Close dialog"
              class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full border border-white/10 text-stone-400 transition hover:border-white/30 hover:text-white"
            >
              ×
            </button>
          </div>
        </div>

        <!-- Modal Content -->
        <div class="px-6 py-5 sm:px-7">
          <p class="text-sm leading-6 text-stone-300">{{ item.message }}</p>

          <!-- 400 Bad Request Field Validation Breakdown -->
          <div
            *ngIf="item.validationErrors && hasValidationErrors(item.validationErrors)"
            class="mt-4 rounded-xl border border-red-500/25 bg-red-950/20 p-4"
          >
            <div class="flex items-center justify-between gap-2 border-b border-red-500/20 pb-2">
              <p class="text-[10px] font-semibold tracking-wider text-rose-300 uppercase">
                Field Validation Details (HTTP 400)
              </p>
              <span class="text-[10px] text-stone-400">
                {{ getValidationEntries(item.validationErrors).length }} issue{{ getValidationEntries(item.validationErrors).length > 1 ? 's' : '' }}
              </span>
            </div>
            <dl class="mt-3 space-y-2.5">
              <div
                *ngFor="let err of getValidationEntries(item.validationErrors)"
                class="flex flex-col gap-0.5 rounded-lg border border-white/5 bg-black/40 p-2.5 sm:flex-row sm:items-baseline sm:gap-2.5"
              >
                <dt class="shrink-0 font-mono text-[11px] font-semibold text-rose-200">
                  {{ err.field }}:
                </dt>
                <dd class="text-xs text-stone-300">{{ err.message }}</dd>
              </div>
            </dl>
          </div>
        </div>

        <!-- Modal Footer Actions -->
        <div class="flex flex-wrap items-center justify-between gap-3 border-t border-white/10 bg-white/[0.02] px-6 py-4 sm:px-7">
          <button
            type="button"
            (click)="feedback.openLog()"
            class="text-[11px] font-medium tracking-wide text-stone-400 underline-offset-4 hover:text-rose-100 hover:underline"
          >
            View Feedback & Error Log ({{ historyCount() }})
          </button>
          <button
            type="button"
            (click)="feedback.closeDialog()"
            class="min-h-10 rounded-lg border border-white/20 bg-white/10 px-6 py-2 text-xs font-semibold tracking-wider text-white transition hover:bg-white hover:text-black"
          >
            DISMISS
          </button>
        </div>
      </div>
    </div>

    <!-- History / Activity Log Modal -->
    <div
      *ngIf="feedback.isLogOpen()"
      role="dialog"
      aria-modal="true"
      aria-labelledby="feedback-log-title"
      class="fixed inset-0 z-[110] flex items-center justify-center overflow-y-auto bg-black/85 p-4 backdrop-blur-md"
      (click)="feedback.closeLog()"
    >
      <div
        class="my-auto max-h-[85vh] w-full max-w-2xl flex flex-col overflow-hidden rounded-2xl border border-white/15 bg-[#100f0e] text-stone-100 shadow-[0_32px_120px_rgba(0,0,0,0.9)]"
        (click)="$event.stopPropagation()"
      >
        <div class="flex items-center justify-between border-b border-white/10 px-6 py-4">
          <div class="flex items-center gap-3">
            <span class="flex h-7 w-7 items-center justify-center rounded-full bg-rose-100/10 text-xs font-mono text-rose-200">📋</span>
            <div>
              <h3 id="feedback-log-title" class="font-sans-clean text-lg font-medium text-white">Feedback & Activity Log</h3>
              <p class="text-[11px] text-stone-400">All backend responses, validations, and operations logged during this session.</p>
            </div>
          </div>
          <button
            type="button"
            (click)="feedback.closeLog()"
            aria-label="Close activity log"
            class="flex h-8 w-8 items-center justify-center rounded-full border border-white/10 text-stone-400 hover:text-white"
          >
            ×
          </button>
        </div>

        <div class="flex-1 overflow-y-auto p-6 space-y-3">
          <div *ngIf="feedback.history().length === 0" class="py-12 text-center text-sm text-stone-500">
            No feedback entries recorded yet.
          </div>
          <article
            *ngFor="let item of feedback.history()"
            class="rounded-xl border border-white/10 bg-white/[0.02] p-4 transition hover:border-white/20"
          >
            <div class="flex flex-wrap items-center justify-between gap-2">
              <div class="flex items-center gap-2">
                <span
                  class="rounded px-2 py-0.5 font-mono text-[10px] font-semibold tracking-wide uppercase"
                  [ngClass]="getStatusPillClasses(item)"
                >
                  {{ getStatusText(item) }}
                </span>
                <h4 class="text-xs font-semibold text-white">{{ item.title }}</h4>
              </div>
              <time class="font-mono text-[10px] text-stone-500">
                {{ item.timestamp | date:'HH:mm:ss' }}
              </time>
            </div>
            <p class="mt-2 text-xs leading-5 text-stone-300">{{ item.message }}</p>

            <div *ngIf="item.validationErrors && hasValidationErrors(item.validationErrors)" class="mt-2.5 rounded bg-black/40 p-2.5 border border-red-500/20">
              <p class="text-[10px] font-semibold text-rose-300">Specified Validation Errors:</p>
              <ul class="mt-1 space-y-1 text-[11px]">
                <li *ngFor="let err of getValidationEntries(item.validationErrors)" class="text-stone-300">
                  <span class="font-mono text-rose-200">{{ err.field }}:</span> {{ err.message }}
                </li>
              </ul>
            </div>
          </article>
        </div>

        <div class="flex items-center justify-between border-t border-white/10 bg-white/[0.02] px-6 py-3.5">
          <button
            type="button"
            (click)="feedback.clearHistory()"
            [disabled]="feedback.history().length === 0"
            class="text-[11px] text-stone-400 hover:text-red-300 disabled:opacity-40 disabled:hover:text-stone-400"
          >
            Clear Log
          </button>
          <button
            type="button"
            (click)="feedback.closeLog()"
            class="rounded-lg border border-white/20 px-4 py-1.5 text-xs text-white hover:bg-white/10"
          >
            CLOSE
          </button>
        </div>
      </div>
    </div>
  `
})
export class FeedbackDialogComponent {
  readonly feedback = inject(FeedbackService);
  readonly dialog = this.feedback.activeDialog;
  readonly toast = this.feedback.toast;
  readonly historyCount = computed(() => this.feedback.history().length);

  hasValidationErrors(errors?: Record<string, string>): boolean {
    return Boolean(errors && Object.keys(errors).length > 0);
  }

  getValidationEntries(errors?: Record<string, string>): { field: string; message: string }[] {
    if (!errors) return [];
    return Object.entries(errors).map(([field, message]) => ({ field, message }));
  }

  getToastClasses(type: FeedbackType): string {
    switch (type) {
      case 'error':
        return 'border-red-500/30 bg-red-950/70 text-red-100';
      case 'success':
        return 'border-emerald-500/30 bg-emerald-950/70 text-emerald-100';
      case 'warning':
        return 'border-amber-500/30 bg-amber-950/70 text-amber-100';
      case 'info':
      default:
        return 'border-sky-500/30 bg-sky-950/70 text-sky-100';
    }
  }

  getDialogBorderClasses(type: FeedbackType): string {
    switch (type) {
      case 'error':
        return 'border-red-500/40 shadow-red-950/30';
      case 'success':
        return 'border-emerald-500/40 shadow-emerald-950/30';
      case 'warning':
        return 'border-amber-500/40 shadow-amber-950/30';
      case 'info':
      default:
        return 'border-sky-500/40 shadow-sky-950/30';
    }
  }

  getIconBadgeClasses(type: FeedbackType): string {
    switch (type) {
      case 'error':
        return 'bg-red-500/20 text-red-300';
      case 'success':
        return 'bg-emerald-500/20 text-emerald-300';
      case 'warning':
        return 'bg-amber-500/20 text-amber-300';
      case 'info':
      default:
        return 'bg-sky-500/20 text-sky-300';
    }
  }

  getStatusPillClasses(item: FeedbackItem): string {
    if (item.status === 400) {
      return 'bg-rose-500/20 text-rose-300 border border-rose-500/30';
    }
    switch (item.type) {
      case 'error':
        return 'bg-red-500/20 text-red-300 border border-red-500/30';
      case 'success':
        return 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30';
      case 'warning':
        return 'bg-amber-500/20 text-amber-300 border border-amber-500/30';
      case 'info':
      default:
        return 'bg-sky-500/20 text-sky-300 border border-sky-500/30';
    }
  }

  getStatusText(item: FeedbackItem): string {
    if (item.status) {
      return `HTTP ${item.status}`;
    }
    return item.type.toUpperCase();
  }

  getIconSymbol(type: FeedbackType): string {
    switch (type) {
      case 'error':
        return '✕';
      case 'success':
        return '✓';
      case 'warning':
        return '!';
      case 'info':
      default:
        return 'i';
    }
  }
}
