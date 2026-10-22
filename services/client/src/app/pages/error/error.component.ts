import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { NavbarComponent } from '../../shared/navbar/navbar.component';

interface ErrorDetails {
  eyebrow: string;
  title: string;
  description: string;
  action: string;
}

const ERROR_DETAILS: Record<number, ErrorDetails> = {
  400: {
    eyebrow: 'INVALID REQUEST',
    title: 'That request could not be understood.',
    description: 'Check the information and try again.',
    action: 'BACK TO HOME'
  },
  401: {
    eyebrow: 'SIGN IN REQUIRED',
    title: 'Your session needs attention.',
    description: 'Sign in to continue. If you were already signed in, your session may have expired.',
    action: 'SIGN IN'
  },
  403: {
    eyebrow: 'ACCESS DENIED',
    title: 'You do not have permission to view this page.',
    description: 'Return to the home page or sign in with an account that has access.',
    action: 'BACK TO HOME'
  },
  404: {
    eyebrow: 'NOT FOUND',
    title: 'We could not find that page.',
    description: 'The link may be out of date, or the page may have moved.',
    action: 'BACK TO HOME'
  },
  408: {
    eyebrow: 'REQUEST TIMED OUT',
    title: 'That took longer than expected.',
    description: 'Please try again in a moment.',
    action: 'BACK TO HOME'
  },
  409: {
    eyebrow: 'REQUEST CONFLICT',
    title: 'This change conflicts with the current information.',
    description: 'Refresh the page and try again.',
    action: 'BACK TO HOME'
  },
  422: {
    eyebrow: 'INVALID INFORMATION',
    title: 'Some information needs to be corrected.',
    description: 'Review your entries and try again.',
    action: 'BACK TO HOME'
  },
  429: {
    eyebrow: 'SLOW DOWN',
    title: 'Too many requests right now.',
    description: 'Please wait a little and try again.',
    action: 'BACK TO HOME'
  },
  500: {
    eyebrow: 'SERVER ERROR',
    title: 'Something went wrong on our side.',
    description: 'Please try again shortly.',
    action: 'BACK TO HOME'
  },
  502: {
    eyebrow: 'SERVICE UNAVAILABLE',
    title: 'We could not reach the service.',
    description: 'Please try again shortly.',
    action: 'BACK TO HOME'
  },
  503: {
    eyebrow: 'SERVICE UNAVAILABLE',
    title: 'TRAVELPLAN is temporarily unavailable.',
    description: 'Check that the services are running, then try again.',
    action: 'BACK TO HOME'
  },
  504: {
    eyebrow: 'GATEWAY TIMEOUT',
    title: 'The service took too long to respond.',
    description: 'Please try again in a moment.',
    action: 'BACK TO HOME'
  }
};

@Component({
  selector: 'app-error',
  standalone: true,
  imports: [CommonModule, RouterLink, NavbarComponent],
  template: `
    <main class="flex min-h-screen items-center justify-center bg-[#080706] px-5 pb-12 pt-24 text-stone-100">
      <app-navbar activePage="home" />
      <section class="w-full max-w-lg border border-white/10 bg-white/[0.025] p-7 text-center sm:p-10">
        <a routerLink="/home" aria-label="TRAVELPLAN home" class="mx-auto mb-8 flex w-fit items-center gap-3 text-white">
          <img src="/travelplan-logo.svg" alt="" class="h-11 w-11" />
          <span class="font-travel text-sm font-bold tracking-[0.2em]">TRAVELPLAN</span>
        </a>
        <p class="text-[10px] tracking-[0.22em] text-rose-100">{{ details().eyebrow }}</p>
        <p class="mt-2 text-xs tracking-[0.12em] text-stone-500">ERROR {{ status() }}</p>
        <h1 class="mt-3 font-sans-clean text-3xl font-light">{{ details().title }}</h1>
        <p class="mx-auto mt-3 max-w-sm text-sm leading-6 text-stone-400">{{ details().description }}</p>
        <a
          [routerLink]="status() === 401 ? '/login' : '/home'"
          class="mt-7 inline-flex min-h-11 items-center justify-center border border-rose-100/60 px-5 py-3 text-xs tracking-[0.12em] text-rose-100 transition hover:bg-rose-100 hover:text-black">
          {{ details().action }}
        </a>
      </section>
    </main>
  `
})
export class ErrorComponent {
  private readonly route = inject(ActivatedRoute);
  readonly status = signal(this.readStatus(this.route.snapshot.paramMap.get('status')));
  readonly details = computed(() => ERROR_DETAILS[this.status()] ?? {
    eyebrow: this.status() >= 500 ? 'SERVER ERROR' : 'REQUEST ERROR',
    title: this.status() >= 500 ? 'The service could not complete your request.' : 'Your request could not be completed.',
    description: 'Please try again, or return to the home page.',
    action: 'BACK TO HOME'
  });

  constructor() {
    this.route.paramMap.subscribe((params) => this.status.set(this.readStatus(params.get('status'))));
  }

  private readStatus(value: string | null): number {
    const status = Number(value);
    return Number.isInteger(status) && status >= 400 && status <= 599 ? status : 404;
  }
}
