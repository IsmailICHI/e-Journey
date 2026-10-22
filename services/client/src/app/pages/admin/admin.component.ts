import { CommonModule } from '@angular/common';
import { AfterViewInit, Component, ElementRef, NgZone, OnDestroy, OnInit, ViewChild, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { NavbarComponent } from '../../shared/navbar/navbar.component';
import { getApiErrorMessage } from '../../core/interceptors/api-error';
import { ItineraryRequest, TravelRequest, TravelResponse } from '../../core/models/travel.model';
import { UserResponse } from '../../core/models/user.model';
import { AuthService } from '../../core/services/auth.service';
import { FeedbackService } from '../../core/services/feedback.service';
import { TravelService } from '../../core/services/travel.service';
import { UserService } from '../../core/services/user.service';

type AdminTab = 'travels' | 'users';

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
    { id: 'travels', label: 'Travels' },
    { id: 'users', label: 'Users' }
  ];

  activeTab: AdminTab = 'travels';
  users: UserResponse[] = [];
  travels: TravelResponse[] = [];
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
  errorMessage = '';
  successMessage = '';

  ngOnInit(): void {
    this.loadUsers();
    this.loadTravels();
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
    } else {
      this.loadTravels();
    }
  }

  setTab(tab: AdminTab): void {
    this.activeTab = tab;
    this.clearMessages();
  }

  showTravel(travel: TravelResponse): void {
    this.selectedTravel = travel;
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
    if (this.savingTravel || this.travelForm.duration === null) {
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
    const itinerary: ItineraryRequest[] = activities.map((activity, index) => ({
      dayNumber: index + 1,
      title: `Day ${index + 1}`,
      description: '',
      activities: [activity]
    }));

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
