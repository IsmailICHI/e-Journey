import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../../shared/navbar/navbar.component';
import { getApiErrorMessage } from '../../core/interceptors/api-error';
import { BookingResponse, FeedbackResponse } from '../../core/models/booking.model';
import { TravelResponse } from '../../core/models/travel.model';
import { BookingService } from '../../core/services/booking.service';
import { PaymentService, PaymentOrderResponse } from '../../core/services/payment.service';
import { TravelService } from '../../core/services/travel.service';

@Component({
  selector: 'app-traveler-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent],
  templateUrl: './traveler-dashboard.component.html'
})
export class TravelerDashboardComponent implements OnInit {
  private readonly travelService = inject(TravelService);
  private readonly bookingService = inject(BookingService);
  private readonly paymentService = inject(PaymentService);
  travels: TravelResponse[] = [];
  bookings: BookingResponse[] = [];
  loading = false;
  loadingBookings = false;
  search = '';
  errorMessage = '';
  successMessage = '';
  readonly bookingTravelIds = new Set<number>();
  readonly feedbackDrafts: Record<number, { rating: number; comment: string }> = {};
  readonly pendingPayments = new Map<number, PaymentOrderResponse>();
  readonly submittingFeedbackIds = new Set<number>();
  selectedReportTravel: TravelResponse | null = null;
  reportType: 'TRAVEL' | 'MANAGER' = 'TRAVEL';
  reportReason = '';
  reportDetails = '';
  submittingReport = false;

  ngOnInit(): void {
    this.loadTravels();
    this.loadBookings();
  }

  get filteredTravels(): TravelResponse[] {
    const query = this.search.trim().toLowerCase();
    if (!query) {
      return this.travels;
    }
    return this.travels.filter((travel) =>
      [travel.title, travel.city, travel.country, ...travel.destinations]
        .some((value) => value.toLowerCase().includes(query)));
  }

  loadTravels(): void {
    this.loading = true;
    this.errorMessage = '';
    this.travelService.getPublicTravels().subscribe({
      next: (travels) => {
        this.travels = travels;
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
    if (this.bookingTravelIds.has(travel.travelId)) {
      return;
    }
    this.bookingTravelIds.add(travel.travelId);
    this.errorMessage = '';
    this.successMessage = '';
    this.bookingService.createBooking(travel.travelId).subscribe({
      next: (booking) => {
        this.successMessage = `Booking #${booking.id} created. Preparing your PayPal checkout…`;
        this.bookingTravelIds.delete(travel.travelId);
        this.loadBookings();
        this.paymentService.createOrder(booking.id).subscribe({
          next: (order) => {
            this.pendingPayments.set(booking.id, order);
            this.successMessage = `Booking #${booking.id} is ready. Continue to PayPal, approve the payment, then confirm it here.`;
          },
          error: (error: unknown) => {
            this.errorMessage = `Your booking was saved, but payment setup failed: ${getApiErrorMessage(error)}`;
          }
        });
      },
      error: (error: unknown) => {
        this.errorMessage = getApiErrorMessage(error);
        this.bookingTravelIds.delete(travel.travelId);
      }
    });
  }

  capturePayment(bookingId: number): void {
    const order = this.pendingPayments.get(bookingId);
    if (!order) {
      return;
    }
    this.paymentService.capture(order.paymentId).subscribe({
      next: () => {
        this.pendingPayments.delete(bookingId);
        this.successMessage = `Payment for booking #${bookingId} was confirmed.`;
        this.loadBookings();
      },
      error: (error: unknown) => {
        this.errorMessage = getApiErrorMessage(error);
      }
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
    const targetId = this.reportType === 'MANAGER' ? travel.createdByUserId : travel.travelId;
    if (targetId === null) {
      this.errorMessage = 'This journey does not have a manager assigned yet.';
      return;
    }
    this.submittingReport = true;
    this.bookingService.submitReport(this.reportType, targetId, this.reportReason, this.reportDetails).subscribe({
      next: () => {
        this.successMessage = 'Your report was sent to the administration team.';
        this.selectedReportTravel = null;
        this.submittingReport = false;
      },
      error: (error: unknown) => {
        this.errorMessage = getApiErrorMessage(error);
        this.submittingReport = false;
      }
    });
  }
}
