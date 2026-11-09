import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../../shared/navbar/navbar.component';
import { getApiErrorMessage } from '../../core/interceptors/api-error';
import { TravelResponse } from '../../core/models/travel.model';
import { BookingService } from '../../core/services/booking.service';
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
  travels: TravelResponse[] = [];
  loading = false;
  search = '';
  errorMessage = '';
  successMessage = '';
  readonly bookingTravelIds = new Set<number>();

  ngOnInit(): void {
    this.loadTravels();
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

  bookTravel(travel: TravelResponse): void {
    if (this.bookingTravelIds.has(travel.travelId)) {
      return;
    }
    this.bookingTravelIds.add(travel.travelId);
    this.errorMessage = '';
    this.successMessage = '';
    this.bookingService.createBooking(travel.travelId).subscribe({
      next: (booking) => {
        this.successMessage = `Booking #${booking.id} was created for ${travel.title}.`;
        this.bookingTravelIds.delete(travel.travelId);
      },
      error: (error: unknown) => {
        this.errorMessage = getApiErrorMessage(error);
        this.bookingTravelIds.delete(travel.travelId);
      }
    });
  }
}
