import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { NavbarComponent } from '../../shared/navbar/navbar.component';
import { getApiErrorMessage } from '../../core/interceptors/api-error';
import { BookingResponse, FeedbackResponse, PaymentAnalyticsResponse } from '../../core/models/booking.model';
import { TravelRequest, TravelResponse } from '../../core/models/travel.model';
import { UserProfileResponse } from '../../core/models/user.model';
import { BookingService } from '../../core/services/booking.service';
import { PaymentService } from '../../core/services/payment.service';
import { TravelService } from '../../core/services/travel.service';
import { UserService } from '../../core/services/user.service';

@Component({
  selector: 'app-travel-manager-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent, RouterLink],
  templateUrl: './travel-manager-dashboard.component.html'
})
export class TravelManagerDashboardComponent implements OnInit {
  private readonly travelService = inject(TravelService);
  private readonly bookingService = inject(BookingService);
  private readonly paymentService = inject(PaymentService);
  private readonly userService = inject(UserService);
  readonly Math = Math;
  travels: TravelResponse[] = [];
  bookings: BookingResponse[] = [];
  feedbackEntries: FeedbackResponse[] = [];
  analytics: PaymentAnalyticsResponse | null = null;
  readonly travelerProfiles: Record<number, UserProfileResponse> = {};
  readonly profileLoadingIds = new Set<number>();
  readonly unsubscribingIds = new Set<number>();
  loading = false;
  loadingBookings = false;
  loadingFeedback = false;
  loadingAnalytics = false;
  saving = false;
  deletingId: number | null = null;
  page = 0;
  totalTravels = 0;
  isEditorOpen = false;
  editingTravelId: number | null = null;
  errorMessage = '';
  successMessage = '';
  form = this.emptyForm();

  get activeSubscribers(): BookingResponse[] {
    return this.bookings.filter((booking) => booking.status !== 'CANCELLED');
  }

  get travelerCount(): number {
    return new Set(this.activeSubscribers.map((booking) => booking.userId)).size;
  }

  get averageRating(): number | null {
    if (this.feedbackEntries.length === 0) {
      return null;
    }
    return this.feedbackEntries.reduce((total, entry) => total + entry.rating, 0) / this.feedbackEntries.length;
  }

  get maxMonthlyIncome(): number {
    return Math.max(0, ...((this.analytics?.monthlyIncome ?? []).map((month) => month.income)));
  }

  travelTitle(travelId: number): string {
    return this.travels.find((travel) => travel.travelId === travelId)?.title ?? `Journey #${travelId}`;
  }

  ngOnInit(): void {
    this.loadTravels();
    this.loadManagerData();
  }

  loadManagerData(): void {
    this.loadManagerBookings();
    this.loadManagerFeedback();
    this.loadManagerAnalytics();
  }

  private loadManagerBookings(): void {
    this.loadingBookings = true;
    this.bookingService.getManagerBookings().subscribe({
      next: (bookings) => {
        this.bookings = bookings;
        this.loadingBookings = false;
        this.loadTravelerProfiles();
      },
      error: (error: unknown) => {
        this.errorMessage = getApiErrorMessage(error);
        this.loadingBookings = false;
      }
    });
  }

  private loadTravelerProfiles(): void {
    this.activeSubscribers.forEach((booking) => {
      if (this.travelerProfiles[booking.id] || this.profileLoadingIds.has(booking.id)) {
        return;
      }
      this.profileLoadingIds.add(booking.id);
      this.userService.getTravelerProfileForBooking(booking.id).subscribe({
        next: (profile) => {
          this.travelerProfiles[booking.id] = profile;
          this.profileLoadingIds.delete(booking.id);
        },
        error: (error: unknown) => {
          this.errorMessage = getApiErrorMessage(error);
          this.profileLoadingIds.delete(booking.id);
        }
      });
    });
  }

  private loadManagerFeedback(): void {
    this.loadingFeedback = true;
    this.bookingService.getManagerFeedback().subscribe({
      next: (entries) => {
        this.feedbackEntries = entries;
        this.loadingFeedback = false;
      },
      error: (error: unknown) => {
        this.errorMessage = getApiErrorMessage(error);
        this.loadingFeedback = false;
      }
    });
  }

  private loadManagerAnalytics(): void {
    this.loadingAnalytics = true;
    this.paymentService.getManagerAnalytics(3).subscribe({
      next: (analytics) => {
        this.analytics = analytics;
        this.loadingAnalytics = false;
      },
      error: (error: unknown) => {
        this.errorMessage = getApiErrorMessage(error);
        this.loadingAnalytics = false;
      }
    });
  }

  unsubscribeTraveler(booking: BookingResponse): void {
    const name = this.travelerProfiles[booking.id]?.fullName ?? `traveler #${booking.userId}`;
    if (!window.confirm(`Unsubscribe ${name} from "${booking.travelTitle ?? 'this journey'}"? Only unpaid bookings can be removed.`)) {
      return;
    }
    this.unsubscribingIds.add(booking.id);
    this.errorMessage = '';
    this.bookingService.unsubscribeTraveler(booking.id).subscribe({
      next: () => {
        this.successMessage = `${name} was unsubscribed from this journey.`;
        this.unsubscribingIds.delete(booking.id);
        this.loadManagerBookings();
      },
      error: (error: unknown) => {
        this.errorMessage = getApiErrorMessage(error);
        this.unsubscribingIds.delete(booking.id);
      }
    });
  }

  get activeThisPage(): number {
    const today = new Date().toISOString().slice(0, 10);
    return this.travels.filter((travel) => !travel.endDate || travel.endDate >= today).length;
  }

  loadTravels(page = this.page): void {
    this.loading = true;
    this.errorMessage = '';
    this.travelService.getTravels(page).subscribe({
      next: (result) => {
        this.travels = result.content;
        this.totalTravels = result.totalElements;
        this.page = result.page;
        this.loading = false;
      },
      error: (error: unknown) => {
        this.errorMessage = getApiErrorMessage(error);
        this.loading = false;
      }
    });
  }

  openCreate(): void {
    this.editingTravelId = null;
    this.form = this.emptyForm();
    this.errorMessage = '';
    this.isEditorOpen = true;
  }

  openEdit(travel: TravelResponse): void {
    this.editingTravelId = travel.travelId;
    this.form = {
      title: travel.title,
      city: travel.city,
      country: travel.country,
      description: travel.description ?? '',
      imageUrl: travel.imageUrl ?? '',
      duration: travel.duration,
      destinations: travel.destinations.join(', '),
      startDate: travel.startDate ?? '',
      endDate: travel.endDate ?? '',
      accommodation: travel.accommodation ?? '',
      transportation: travel.transportation ?? '',
      paymentMethod: travel.paymentMethod ?? 'PayPal',
      priceMad: travel.priceMad ?? 0
    };
    this.errorMessage = '';
    this.isEditorOpen = true;
  }

  closeEditor(): void {
    if (!this.saving) {
      this.isEditorOpen = false;
    }
  }

  saveTravel(): void {
    const request: TravelRequest = {
      ...this.form,
      destinations: this.form.destinations.split(',').map((item) => item.trim()).filter(Boolean),
      itinerary: []
    };
    this.saving = true;
    this.errorMessage = '';
    const save = this.editingTravelId === null
      ? this.travelService.createTravel(request)
      : this.travelService.updateTravel(this.editingTravelId, request);
    save.subscribe({
      next: (travel) => {
        this.successMessage = `${travel.title} ${this.editingTravelId === null ? 'created' : 'updated'} successfully.`;
        this.isEditorOpen = false;
        this.saving = false;
        this.loadTravels(this.page);
      },
      error: (error: unknown) => {
        this.errorMessage = getApiErrorMessage(error);
        this.saving = false;
      }
    });
  }

  deleteTravel(travel: TravelResponse): void {
    if (!window.confirm(`Delete "${travel.title}"? This cannot be undone.`)) {
      return;
    }
    this.deletingId = travel.travelId;
    this.errorMessage = '';
    this.travelService.deleteTravel(travel.travelId).subscribe({
      next: () => {
        this.successMessage = `${travel.title} was deleted.`;
        this.deletingId = null;
        if (this.travels.length === 1 && this.page > 0) {
          this.loadTravels(this.page - 1);
        } else {
          this.loadTravels(this.page);
        }
      },
      error: (error: unknown) => {
        this.errorMessage = getApiErrorMessage(error);
        this.deletingId = null;
      }
    });
  }

  private emptyForm() {
    return {
      title: '',
      city: '',
      country: '',
      description: '',
      imageUrl: '',
      duration: 1,
      destinations: '',
      startDate: '',
      endDate: '',
      accommodation: '',
      transportation: '',
      paymentMethod: 'PayPal',
      priceMad: 0
    };
  }
}
