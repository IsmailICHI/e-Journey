import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../../shared/navbar/navbar.component';
import { getApiErrorMessage } from '../../core/interceptors/api-error';
import { TravelRequest, TravelResponse } from '../../core/models/travel.model';
import { TravelService } from '../../core/services/travel.service';

@Component({
  selector: 'app-travel-manager-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent],
  templateUrl: './travel-manager-dashboard.component.html'
})
export class TravelManagerDashboardComponent implements OnInit {
  private readonly travelService = inject(TravelService);
  readonly Math = Math;
  travels: TravelResponse[] = [];
  loading = false;
  saving = false;
  deletingId: number | null = null;
  page = 0;
  totalTravels = 0;
  isEditorOpen = false;
  editingTravelId: number | null = null;
  errorMessage = '';
  successMessage = '';
  form = this.emptyForm();

  ngOnInit(): void {
    this.loadTravels();
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
