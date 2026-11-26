import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, catchError, debounceTime, distinctUntilChanged, of, switchMap, take } from 'rxjs';
import { NavbarComponent } from '../../shared/navbar/navbar.component';
import { getApiErrorMessage } from '../../core/interceptors/api-error';
import { BookingResponse, FeedbackResponse, ReportResponse, TravelerStatisticsResponse } from '../../core/models/booking.model';
import { TravelResponse } from '../../core/models/travel.model';
import { BookingService } from '../../core/services/booking.service';
import { AuthService } from '../../core/services/auth.service';
import { ConfirmationService } from '../../core/services/confirmation.service';
import { PaymentService, PaymentOrderResponse } from '../../core/services/payment.service';
import { TravelService } from '../../core/services/travel.service';

@Component({
  selector: 'app-traveler-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent, RouterLink],
  templateUrl: './traveler-dashboard.component.html'
})
export class TravelerDashboardComponent implements OnInit {
  private readonly travelService = inject(TravelService);
  private readonly bookingService = inject(BookingService);
  private readonly authService = inject(AuthService);
  private readonly confirmation = inject(ConfirmationService);
  private readonly paymentService = inject(PaymentService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly searchChanges = new Subject<string>();
  travels: TravelResponse[] = [];
  searchResults: TravelResponse[] = [];
  searchSuggestions: TravelResponse[] = [];
  recommendations: TravelResponse[] = [];
  bookings: BookingResponse[] = [];
  statistics: TravelerStatisticsResponse | null = null;
  reports: ReportResponse[] = [];
  selectedTravel: TravelResponse | null = null;
  loading = false;
  loadingBookings = false;
  search = '';
  errorMessage = '';
  successMessage = '';
  readonly bookingTravelIds = new Set<number>();
  readonly feedbackDrafts: Record<number, { rating: number; comment: string }> = {};
  readonly submittingFeedbackIds = new Set<number>();
  selectedReportTravel: TravelResponse | null = null;
  reportType: 'TRAVEL' | 'MANAGER' | 'TRAVELER' = 'TRAVEL';
  reportTravelerId = '';
  reportReason = '';
  reportDetails = '';
  submittingReport = false;

  ngOnInit(): void {
    this.searchChanges.pipe(
      debounceTime(250),
      distinctUntilChanged(),
      switchMap((query) => query.length < 2
        ? of(this.travels)
        : this.travelService.searchPublicTravels(query).pipe(
            catchError((error: unknown) => {
              this.errorMessage = getApiErrorMessage(error);
              return of([]);
            })
          )),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((travels) => {
      this.searchResults = this.visibleTravels(travels);
      this.searchSuggestions = this.search.trim().length >= 2 ? this.searchResults.slice(0, 5) : [];
    });
    this.loadTravels();
    this.loadBookings();
    this.loadRecommendations();
    this.loadStatistics();
    this.loadReports();
    this.route.queryParamMap.pipe(take(1)).subscribe((params) => {
      const sessionId = params.get('stripe_session_id');
      if (sessionId) {
        this.paymentService.completeStripeSession(sessionId).subscribe({
          next: () => {
            this.successMessage = 'Your Stripe payment was confirmed.';
            this.loadBookings();
            this.clearPaymentQueryParams();
          },
          error: (error: unknown) => {
            this.errorMessage = getApiErrorMessage(error);
            this.clearPaymentQueryParams();
          }
        });
      } else if (params.get('payment_cancelled') === 'true') {
        this.successMessage = 'Checkout was cancelled. Your booking is still awaiting payment.';
        this.clearPaymentQueryParams();
      }
    });
  }

  onSearchChange(query: string): void {
    this.searchChanges.next(query.trim().toLowerCase());
  }

  selectSuggestion(travel: TravelResponse): void {
    this.search = travel.title;
    this.searchSuggestions = [];
    this.searchResults = [travel];
  }

  showTravelDetails(travel: TravelResponse): void {
    this.selectedTravel = travel;
  }

  closeTravelDetails(): void {
    this.selectedTravel = null;
  }

  activitiesFor(travel: TravelResponse): string[] {
    return travel.itinerary.flatMap((entry) => entry.activities);
  }

  get todayUtcDate(): string {
    return new Date().toISOString().slice(0, 10);
  }

  private loadRecommendations(): void {
    this.travelService.getRecommendations().subscribe({
      next: (recommendations) => { this.recommendations = this.visibleTravels(recommendations); },
      error: (error: unknown) => { this.errorMessage = getApiErrorMessage(error); }
    });
  }

  private loadStatistics(): void {
    this.bookingService.getMyStatistics().subscribe({
      next: (statistics) => { this.statistics = statistics; },
      error: (error: unknown) => { this.errorMessage = getApiErrorMessage(error); }
    });
  }

  private loadReports(): void {
    this.bookingService.getMyReports().subscribe({
      next: (reports) => { this.reports = reports; },
      error: (error: unknown) => { this.errorMessage = getApiErrorMessage(error); }
    });
  }

  private clearPaymentQueryParams(): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { stripe_session_id: null, payment_cancelled: null },
      queryParamsHandling: 'merge',
      replaceUrl: true
    });
  }

  get filteredTravels(): TravelResponse[] {
    const query = this.search.trim().toLowerCase();
    if (!query) {
      return this.travels;
    }
    return this.searchResults;
  }

  loadTravels(): void {
    this.loading = true;
    this.errorMessage = '';
    this.travelService.getPublicTravels().subscribe({
      next: (travels) => {
        this.travels = this.visibleTravels(travels);
        if (this.search.trim().length < 2) {
          this.searchResults = this.travels;
        }
        this.loading = false;
      },
      error: (error: unknown) => {
        this.errorMessage = getApiErrorMessage(error);
        this.loading = false;
      }
    });
  }

  loadBookings(): void {
    this.loadingBookings = true;
    this.bookingService.getMyBookings().subscribe({
      next: (bookings) => {
        this.bookings = bookings;
        bookings.forEach((booking) => {
          this.feedbackDrafts[booking.id] ??= { rating: 5, comment: '' };
        });
        this.loadingBookings = false;
      },
      error: (error: unknown) => {
        this.errorMessage = getApiErrorMessage(error);
        this.loadingBookings = false;
      }
    });
  }

  bookTravel(travel: TravelResponse): void {
    if (this.bookingTravelIds.has(travel.travelId) || !this.canBookTravel(travel)) {
      return;
    }
    this.bookingTravelIds.add(travel.travelId);
    this.errorMessage = '';
    this.successMessage = '';
    this.bookingService.createBooking(travel.travelId).subscribe({
      next: (booking) => {
        this.successMessage = `Booking #${booking.id} created. Preparing your ${booking.paymentMethod ?? 'PayPal'} checkout…`;
        this.bookingTravelIds.delete(travel.travelId);
        this.loadBookings();
        void this.router.navigate(['/payment', booking.id]);
      },
      error: (error: unknown) => {
        this.errorMessage = getApiErrorMessage(error);
        this.bookingTravelIds.delete(travel.travelId);
      }
    });
  }

  isOwnTravel(travel: TravelResponse): boolean {
    return this.authService.isTravelManager
      && travel.createdByUserId !== null
      && travel.createdByUserId === this.authService.userId;
  }

  travelAvailabilityLabel(travel: TravelResponse): string {
    const today = this.todayUtcDate;
    if (travel.endDate && travel.endDate < today) {
      return 'JOURNEY FINISHED';
    }
    if (travel.startDate && travel.startDate <= today) {
      return travel.endDate && travel.endDate >= today ? 'JOURNEY IN PROGRESS' : 'ALREADY DEPARTED';
    }
    return 'DEPARTURE TOO SOON';
  }

  travelActionLabel(travel: TravelResponse, availableLabel: string): string {
    if (this.bookingTravelIds.has(travel.travelId)) {
      return 'RESERVING…';
    }
    if (!travel.startDate) {
      return 'DATES PENDING';
    }
    if (travel.startDate <= this.todayUtcDate) {
      return this.travelAvailabilityLabel(travel);
    }
    if (!travel.priceMad || travel.priceMad <= 0) {
      return 'PRICE PENDING';
    }
    return this.canBookTravel(travel) ? availableLabel : 'DEPARTURE TOO SOON';
  }

  canBookTravel(travel: TravelResponse): boolean {
    if (this.isOwnTravel(travel) || !travel.startDate || !travel.priceMad || travel.priceMad <= 0) {
      return false;
    }
    const today = new Date(`${this.todayUtcDate}T00:00:00Z`);
    const cutoff = new Date(Date.UTC(today.getUTCFullYear(), today.getUTCMonth(), today.getUTCDate() + 3));
    return travel.startDate > cutoff.toISOString().slice(0, 10);
  }

  private visibleTravels(travels: TravelResponse[]): TravelResponse[] {
    return travels.filter((travel) => !this.isOwnTravel(travel));
  }

  canCancelSubscription(booking: BookingResponse): boolean {
    if (booking.status !== 'PENDING_PAYMENT' || !booking.departureDate) {
      return false;
    }
    const today = new Date();
    const cutoff = new Date(Date.UTC(today.getUTCFullYear(), today.getUTCMonth(), today.getUTCDate() + 3));
    return booking.departureDate > cutoff.toISOString().slice(0, 10);
  }

  canLeaveFeedback(booking: BookingResponse): boolean {
    return booking.status === 'CONFIRMED'
      && !!booking.departureDate
      && booking.departureDate < new Date().toISOString().slice(0, 10);
  }

  async cancelSubscription(booking: BookingResponse): Promise<void> {
    if (!this.canCancelSubscription(booking)) {
      return;
    }
    const confirmed = await this.confirmation.confirm({
      title: 'Cancel this booking?',
      message: `Cancel your unpaid booking for "${booking.travelTitle ?? 'this journey'}"?`,
      confirmLabel: 'CANCEL BOOKING',
      tone: 'danger'
    });
    if (!confirmed) {
      return;
    }
    this.bookingService.cancelMySubscription(booking.id).subscribe({
      next: () => {
        this.successMessage = 'Your subscription was cancelled.';
        this.loadBookings();
        this.loadStatistics();
      },
      error: (error: unknown) => { this.errorMessage = getApiErrorMessage(error); }
    });
  }

  submitFeedback(booking: BookingResponse): void {
    const draft = this.feedbackDrafts[booking.id];
    if (!draft || !draft.comment.trim() || this.submittingFeedbackIds.has(booking.id)) {
      return;
    }
    this.submittingFeedbackIds.add(booking.id);
    this.bookingService.submitFeedback(booking.id, draft.rating, draft.comment.trim()).subscribe({
      next: (_feedback: FeedbackResponse) => {
        this.successMessage = 'Thank you for sharing your feedback.';
        this.submittingFeedbackIds.delete(booking.id);
        this.loadBookings();
      },
      error: (error: unknown) => {
        this.errorMessage = getApiErrorMessage(error);
        this.submittingFeedbackIds.delete(booking.id);
      }
    });
  }

  openReport(travel: TravelResponse): void {
    this.selectedReportTravel = travel;
    this.reportType = 'TRAVEL';
    this.reportTravelerId = '';
    this.reportReason = '';
    this.reportDetails = '';
  }

  closeReport(): void {
    if (!this.submittingReport) {
      this.selectedReportTravel = null;
    }
  }

  submitReport(): void {
    const travel = this.selectedReportTravel;
    if (!travel || !this.reportReason.trim() || !this.reportDetails.trim()) {
      return;
    }
    const targetId = this.reportType === 'MANAGER'
      ? travel.createdByUserId
      : this.reportType === 'TRAVELER'
        ? Number(this.reportTravelerId)
        : travel.travelId;
    if (targetId === null) {
      this.errorMessage = 'This journey does not have a manager assigned yet.';
      return;
    }
    if (!Number.isSafeInteger(targetId) || targetId <= 0) {
      this.errorMessage = 'Enter a valid traveler account ID.';
      return;
    }
    this.submittingReport = true;
    this.bookingService.submitReport(this.reportType, targetId, this.reportReason, this.reportDetails).subscribe({
      next: () => {
        this.successMessage = 'Your report was sent to the administration team.';
        this.selectedReportTravel = null;
        this.submittingReport = false;
        this.loadReports();
      },
      error: (error: unknown) => {
        this.errorMessage = getApiErrorMessage(error);
        this.submittingReport = false;
      }
    });
  }
}
