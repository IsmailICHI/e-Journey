import { CommonModule } from '@angular/common';
import { AfterViewInit, Component, ElementRef, NgZone, OnDestroy, OnInit, ViewChild, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { NavbarComponent } from '../../shared/navbar/navbar.component';
import { getApiErrorMessage } from '../../core/interceptors/api-error';
import { ItineraryRequest, TravelRequest, TravelResponse } from '../../core/models/travel.model';
import { UserResponse } from '../../core/models/user.model';
import { BookingResponse, FeedbackResponse, PaymentAnalyticsResponse, ReportResponse } from '../../core/models/booking.model';
import { AuthService } from '../../core/services/auth.service';
import { BookingService } from '../../core/services/booking.service';
import { FeedbackService } from '../../core/services/feedback.service';
import { PaymentOrderResponse, PaymentService } from '../../core/services/payment.service';
import { TravelService } from '../../core/services/travel.service';
import { UserService } from '../../core/services/user.service';

type AdminTab = 'overview' | 'travels' | 'history' | 'feedback' | 'reports' | 'users';

interface TravelFormValue {
  title: string;
  city: string;
  country: string;
  description: string;
  imageUrl: string;
  duration: number | null;
  destinations: string;
  startDate: string;
  endDate: string;
  accommodation: string;
  transportation: string;
  paymentMethod: string;
  priceMad: number | null;
  activities: string;
}

type DeleteConfirmation =
  | { type: 'travel'; id: number; name: string; detail: string }
  | { type: 'user'; id: number; name: string; detail: string };

const emptyTravelForm = (): TravelFormValue => ({
  title: '',
  city: '',
  country: '',
  description: '',
  imageUrl: '',
  duration: null,
  destinations: '',
  startDate: '',
  endDate: '',
  accommodation: '',
  transportation: '',
  paymentMethod: '',
  priceMad: null,
  activities: ''
});

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent, RouterLink],
  templateUrl: './admin.component.html'
})
export class AdminComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly userService = inject(UserService);
  private readonly travelService = inject(TravelService);
  private readonly bookingService = inject(BookingService);
  private readonly paymentService = inject(PaymentService);
  private readonly authService = inject(AuthService);
  readonly feedback = inject(FeedbackService);
  private readonly ngZone = inject(NgZone);
  private sentinelObserver: IntersectionObserver | null = null;
  private sentinel: ElementRef<HTMLDivElement> | undefined;

  @ViewChild('loadMoreSentinel')
  set loadMoreSentinel(element: ElementRef<HTMLDivElement> | undefined) {
    this.sentinel = element;
    if (element && this.sentinelObserver) {
      this.sentinelObserver.observe(element.nativeElement);
    }
  }

  readonly adminEmail = this.authService.email;
  readonly tabs: { id: AdminTab; label: string }[] = [
    { id: 'overview', label: 'Overview' },
    { id: 'travels', label: 'Travels' },
    { id: 'history', label: 'Travel history' },
    { id: 'feedback', label: 'Feedback' },
    { id: 'reports', label: 'Reports' },
    { id: 'users', label: 'Users' }
  ];
  readonly roleOptions = ['ADMIN', 'TRAVEL_MANAGER', 'TRAVELER'] as const;

  activeTab: AdminTab = 'overview';
  users: UserResponse[] = [];
  travels: TravelResponse[] = [];
  allTravelRecords: TravelResponse[] = [];
  bookings: BookingResponse[] = [];
  feedbackEntries: FeedbackResponse[] = [];
  reports: ReportResponse[] = [];
  paymentRecords: PaymentOrderResponse[] = [];
  paymentAnalytics: PaymentAnalyticsResponse | null = null;
  loadingOversight = false;
  updatingReportIds = new Set<number>();
  selectedTravel: TravelResponse | null = null;
  isTravelEditorOpen = false;
  travelForm = emptyTravelForm();
  editingTravelId: number | null = null;
  loadingUsers = false;
  loadingTravels = false;
  userPage = 0;
  travelPage = 0;
  hasMoreUsers = true;
  hasMoreTravels = true;
  savingTravel = false;
  pendingDelete: DeleteConfirmation | null = null;
  deleting = false;
  updatingRoleIds = new Set<number>();
  errorMessage = '';
  successMessage = '';

  ngOnInit(): void {
    this.loadUsers();
    this.loadTravels();
    this.loadOversight();
  }

  ngAfterViewInit(): void {
    if (typeof IntersectionObserver === 'undefined') {
      return;
    }
    this.sentinelObserver = new IntersectionObserver((entries) => {
      if (entries.some((entry) => entry.isIntersecting)) {
        this.ngZone.run(() => this.loadNextPage());
      }
    }, { rootMargin: '200px' });
    if (this.sentinel) {
      this.sentinelObserver.observe(this.sentinel.nativeElement);
    }
  }

  ngOnDestroy(): void {
    this.sentinelObserver?.disconnect();
  }

  loadUsers(): void {
    if (this.loadingUsers || !this.hasMoreUsers) {
      return;
    }
    this.loadingUsers = true;
    this.userService.getUsers(this.userPage).subscribe({
      next: (response) => {
        const currentIds = new Set(this.users.map((user) => user.id));
        this.users = [
          ...this.users,
          ...response.content.filter((user) => user.id !== this.authService.userId && !currentIds.has(user.id))
        ];
        this.userPage = response.page + 1;
        this.hasMoreUsers = !response.last;
        this.loadingUsers = false;
        this.refreshSentinel();
        if (this.activeTab === 'overview' && this.hasMoreUsers) {
          this.loadUsers();
        }
      },
      error: (error: unknown) => {
        this.errorMessage = getApiErrorMessage(error);
        this.loadingUsers = false;
      }
    });
  }

  loadTravels(): void {
    if (this.loadingTravels || !this.hasMoreTravels) {
      return;
    }
    this.loadingTravels = true;
    this.travelService.getTravels(this.travelPage).subscribe({
      next: (response) => {
        const currentIds = new Set(this.travels.map((travel) => travel.travelId));
        this.travels = [
          ...this.travels,
          ...response.content.filter((travel) => !currentIds.has(travel.travelId))
        ];
        this.travelPage = response.page + 1;
        this.hasMoreTravels = !response.last;
        this.loadingTravels = false;
        this.refreshSentinel();
      },
      error: (error: unknown) => {
        this.errorMessage = getApiErrorMessage(error);
        this.loadingTravels = false;
      }
    });
  }

  loadNextPage(): void {
    if (this.activeTab === 'users') {
      this.loadUsers();
    } else if (this.activeTab === 'travels') {
      this.loadTravels();
    }
  }

  loadOversight(): void {
    this.loadingOversight = true;
    let remaining = 5;
    const finished = (): void => {
      remaining -= 1;
      this.loadingOversight = remaining > 0;
    };
    this.bookingService.getAllBookings().subscribe({
      next: (bookings) => { this.bookings = bookings; finished(); },
      error: (error: unknown) => { this.errorMessage = getApiErrorMessage(error); finished(); }
    });
    this.bookingService.getFeedback().subscribe({
      next: (entries) => { this.feedbackEntries = entries; finished(); },
      error: (error: unknown) => { this.errorMessage = getApiErrorMessage(error); finished(); }
    });
    this.bookingService.getReports().subscribe({
      next: (reports) => { this.reports = reports; finished(); },
      error: (error: unknown) => { this.errorMessage = getApiErrorMessage(error); finished(); }
    });
    this.paymentService.getAdminAnalytics(3).subscribe({
      next: (analytics) => { this.paymentAnalytics = analytics; finished(); },
      error: (error: unknown) => { this.errorMessage = getApiErrorMessage(error); finished(); }
    });
    this.paymentService.getAdminPaymentHistory().subscribe({
      next: (payments) => { this.paymentRecords = payments; finished(); },
      error: (error: unknown) => { this.errorMessage = getApiErrorMessage(error); finished(); }
    });
    this.travelService.getTravelAnalytics().subscribe({
      next: (travels) => { this.allTravelRecords = travels; },
      error: (error: unknown) => { this.errorMessage = getApiErrorMessage(error); }
    });
  }

  get managerRankings(): Array<{
    id: number;
    name: string;
    travels: number;
    bookings: number;
    income: number;
    averageRating: number;
    score: number;
  }> {
    const managers = this.users.filter((user) => user.role === 'TRAVEL_MANAGER');
    const rows = managers.map((manager) => {
      const managedTravels = this.allTravelRecords.filter((travel) => travel.createdByUserId === manager.id);
      const managedBookings = this.bookings.filter((booking) => booking.managerId === manager.id);
      const ratings = this.feedbackEntries.filter((entry) => entry.managerId === manager.id);
      const income = this.paymentAnalytics?.managerIncome.find((entry) => entry.managerId === manager.id)?.income ?? 0;
      const averageRating = ratings.length
        ? ratings.reduce((sum, entry) => sum + entry.rating, 0) / ratings.length
        : 0;
      return {
        id: manager.id,
        name: manager.fullName,
        travels: managedTravels.length,
        bookings: managedBookings.length,
        income,
        averageRating,
        score: 0
      };
    });
    const maxIncome = Math.max(0, ...rows.map((row) => row.income));
    const maxBookings = Math.max(0, ...rows.map((row) => row.bookings));
    const maxTravels = Math.max(0, ...rows.map((row) => row.travels));
    return rows.map((row) => ({
      ...row,
      score: Math.round(
        (row.averageRating / 5 * 50)
        + (maxIncome ? row.income / maxIncome * 30 : 0)
        + (maxBookings ? row.bookings / maxBookings * 10 : 0)
        + (maxTravels ? row.travels / maxTravels * 10 : 0)
      )
    })).sort((left, right) => right.score - left.score || right.income - left.income);
  }

  get topTravels(): Array<{ title: string; income: number; bookings: number }> {
    const bookingsByTravel = new Map<number, number>();
    this.bookings.forEach((booking) => bookingsByTravel.set(
      booking.travelId,
      (bookingsByTravel.get(booking.travelId) ?? 0) + 1
    ));
    return (this.paymentAnalytics?.topTravels ?? []).map((travel) => ({
      title: travel.title,
      income: travel.income,
      bookings: bookingsByTravel.get(travel.travelId) ?? travel.payments
    })).slice(0, 5);
  }

  get openReportCount(): number {
    return this.reports.filter((report) => report.status === 'OPEN').length;
  }

  managerName(managerId: number | null): string {
    if (managerId === null) {
      return 'Administrator / legacy listing';
    }
    return this.users.find((user) => user.id === managerId)?.fullName ?? `Manager #${managerId}`;
  }

  travelerName(travelerId: number): string {
    return this.users.find((user) => user.id === travelerId)?.fullName ?? `Traveler #${travelerId}`;
  }

  travelName(travelId: number): string {
    const travel = this.allTravelRecords.find((entry) => entry.travelId === travelId);
    return travel ? `${travel.title} · ${travel.city}, ${travel.country}` : `Travel #${travelId}`;
  }

  bookingPaymentStatus(booking: BookingResponse): string {
    const payment = this.paymentRecords.find((entry) => entry.bookingId === booking.id);
    if (payment?.status === 'COMPLETED') {
      return 'PAID';
    }
    return payment ? `PAYMENT ${payment.status}` : booking.status.replaceAll('_', ' ');
  }

  incomeBarWidth(income: number): string {
    const max = Math.max(1, ...(this.paymentAnalytics?.monthlyIncome.map((month) => month.income) ?? []));
    return `${Math.max(3, income / max * 100)}%`;
  }

  updateReportStatus(report: ReportResponse): void {
    const status = report.status === 'OPEN' ? 'CLOSED' : 'OPEN';
    this.updatingReportIds.add(report.id);
    this.bookingService.updateReportStatus(report.id, status).subscribe({
      next: (updated) => {
        this.reports = this.reports.map((entry) => entry.id === updated.id ? updated : entry);
        this.updatingReportIds.delete(report.id);
      },
      error: (error: unknown) => {
        this.errorMessage = getApiErrorMessage(error);
        this.updatingReportIds.delete(report.id);
      }
    });
  }

  setTab(tab: AdminTab): void {
    this.activeTab = tab;
    this.clearMessages();
  }

  showTravel(travel: TravelResponse): void {
    this.selectedTravel = travel;
  }

  activitiesFor(travel: TravelResponse): string[] {
    return travel.itinerary.flatMap((entry) => entry.activities);
  }

  closeTravelDetails(): void {
    this.selectedTravel = null;
  }

  startNewTravel(): void {
    this.editingTravelId = null;
    this.travelForm = emptyTravelForm();
    this.selectedTravel = null;
    this.isTravelEditorOpen = true;
    this.clearMessages();
  }

  editTravel(travel: TravelResponse): void {
    this.selectedTravel = null;
    this.editingTravelId = travel.travelId;
    this.isTravelEditorOpen = true;
    this.travelForm = {
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
      paymentMethod: travel.paymentMethod ?? '',
      priceMad: travel.priceMad,
      activities: travel.itinerary.flatMap((day) => day.activities).join('\n')
    };
    this.clearMessages();
  }

  cancelTravelEdit(): void {
    this.editingTravelId = null;
    this.isTravelEditorOpen = false;
    this.travelForm = emptyTravelForm();
    this.clearMessages();
  }

  saveTravel(): void {
    if (this.savingTravel
      || this.travelForm.duration === null
      || this.travelForm.priceMad === null
      || this.travelForm.priceMad <= 0) {
      return;
    }

    const request = this.toTravelRequest();
    this.savingTravel = true;
    this.clearMessages();
    const saveRequest = this.editingTravelId === null
      ? this.travelService.createTravel(request)
      : this.travelService.updateTravel(this.editingTravelId, request);

    saveRequest.subscribe({
      next: (savedTravel) => {
        this.travels = this.editingTravelId === null
          ? [...this.travels, savedTravel]
          : this.travels.map((travel) => travel.travelId === savedTravel.travelId ? savedTravel : travel);
        const actionTitle = this.editingTravelId === null ? 'Travel Created' : 'Travel Updated';
        const actionMessage = this.editingTravelId === null
          ? `"${savedTravel.title}" was successfully created.`
          : `"${savedTravel.title}" was successfully updated.`;
        this.successMessage = actionMessage;
        this.feedback.showSuccess(actionMessage, actionTitle);
        this.editingTravelId = null;
        this.isTravelEditorOpen = false;
        this.travelForm = emptyTravelForm();
        this.savingTravel = false;
      },
      error: (error: unknown) => {
        this.errorMessage = getApiErrorMessage(error);
        this.feedback.showFromApiError(error, this.editingTravelId === null ? 'Create Travel Failed' : 'Update Travel Failed');
        this.savingTravel = false;
      }
    });
  }

  deleteTravel(travel: TravelResponse): void {
    this.pendingDelete = {
      type: 'travel',
      id: travel.travelId,
      name: travel.title,
      detail: `${travel.city}, ${travel.country}`
    };
    this.clearMessages();
  }

  updateUserRole(user: UserResponse, nextRole: string): void {
    if (!nextRole || nextRole === user.role || this.updatingRoleIds.has(user.id)) {
      return;
    }

    this.updatingRoleIds.add(user.id);
    this.clearMessages();
    this.userService.updateUserRole(user.id, nextRole).subscribe({
      next: (updatedUser) => {
        this.users = this.users.map((item) => item.id === user.id
          ? { ...item, role: updatedUser.role }
          : item);
        this.successMessage = `User role changed to ${updatedUser.role}. The next login will use the updated permissions.`;
        this.feedback.showSuccess(`Role for "${user.fullName}" updated to ${updatedUser.role}.`, 'Role Updated');
        this.updatingRoleIds.delete(user.id);
      },
      error: (error: unknown) => {
        this.errorMessage = getApiErrorMessage(error);
        this.feedback.showFromApiError(error, 'Role Update Failed');
        this.users = this.users.map((item) => item.id === user.id ? { ...item, role: user.role } : item);
        this.updatingRoleIds.delete(user.id);
      }
    });
  }

  deleteUser(user: UserResponse): void {
    this.pendingDelete = {
      type: 'user',
      id: user.id,
      name: user.fullName,
      detail: user.email
    };
    this.clearMessages();
  }

  cancelDelete(): void {
    if (!this.deleting) {
      this.pendingDelete = null;
    }
  }

  confirmDelete(): void {
    const confirmation = this.pendingDelete;
    if (!confirmation || this.deleting) {
      return;
    }

    this.deleting = true;
    this.clearMessages();
    const deleteRequest = confirmation.type === 'travel'
      ? this.travelService.deleteTravel(confirmation.id)
      : this.userService.deleteUser(confirmation.id);

    deleteRequest.subscribe({
      next: () => {
        if (confirmation.type === 'travel') {
          this.travels = this.travels.filter((item) => item.travelId !== confirmation.id);
          this.travelPage = Math.floor(this.travels.length / 5);
          this.hasMoreTravels = true;
          if (this.editingTravelId === confirmation.id) {
            this.cancelTravelEdit();
          }
          this.successMessage = 'Travel deleted.';
          this.feedback.showSuccess(`Travel "${confirmation.name}" was permanently deleted.`, 'Travel Deleted');
        } else {
          this.users = this.users.filter((item) => item.id !== confirmation.id);
          this.userPage = Math.floor(this.users.length / 5);
          this.hasMoreUsers = true;
          this.successMessage = 'User deleted.';
          this.feedback.showSuccess(`User "${confirmation.name}" (${confirmation.detail}) was permanently deleted.`, 'User Deleted');
        }
        this.pendingDelete = null;
        this.deleting = false;
      },
      error: (error: unknown) => {
        this.errorMessage = getApiErrorMessage(error);
        this.feedback.showFromApiError(error, `Failed to Delete ${confirmation.type === 'travel' ? 'Travel' : 'User'}`);
        this.deleting = false;
      }
    });
  }

  private toTravelRequest(): TravelRequest {
    const activities = this.travelForm.activities
      .split(/\r?\n/)
      .map((activity) => activity.trim())
      .filter(Boolean);
    const itinerary: ItineraryRequest[] = activities.length
      ? [{
          dayNumber: 1,
          title: 'Activities',
          description: '',
          activities
        }]
      : [];

    return {
      title: this.travelForm.title.trim(),
      city: this.travelForm.city.trim(),
      country: this.travelForm.country.trim(),
      description: this.travelForm.description.trim(),
      imageUrl: this.travelForm.imageUrl.trim(),
      duration: Number(this.travelForm.duration),
      destinations: this.travelForm.destinations.split(',').map((item) => item.trim()).filter(Boolean),
      startDate: this.travelForm.startDate || null,
      endDate: this.travelForm.endDate || null,
      accommodation: this.travelForm.accommodation.trim(),
      transportation: this.travelForm.transportation.trim(),
      paymentMethod: this.travelForm.paymentMethod,
      priceMad: Number(this.travelForm.priceMad),
      itinerary
    };
  }

  private clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }

  private refreshSentinel(): void {
    if (this.sentinel && this.sentinelObserver) {
      this.sentinelObserver.unobserve(this.sentinel.nativeElement);
      this.sentinelObserver.observe(this.sentinel.nativeElement);
    }
  }
}
