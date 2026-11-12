import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { NavbarComponent } from '../../shared/navbar/navbar.component';
import { getApiErrorMessage } from '../../core/interceptors/api-error';
import { FeedbackResponse, ManagerPublicStatisticsResponse } from '../../core/models/booking.model';
import { TravelResponse } from '../../core/models/travel.model';
import { PublicManagerResponse } from '../../core/models/user.model';
import { BookingService } from '../../core/services/booking.service';
import { TravelService } from '../../core/services/travel.service';
import { UserService } from '../../core/services/user.service';

@Component({
  selector: 'app-manager-profile',
  standalone: true,
  imports: [CommonModule, NavbarComponent, RouterLink],
  templateUrl: './manager-profile.component.html'
})
export class ManagerProfileComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly userService = inject(UserService);
  private readonly bookingService = inject(BookingService);
  private readonly travelService = inject(TravelService);

  manager: PublicManagerResponse | null = null;
  statistics: ManagerPublicStatisticsResponse | null = null;
  feedback: FeedbackResponse[] = [];
  travels: TravelResponse[] = [];
  loading = true;
  errorMessage = '';

  ngOnInit(): void {
    const managerId = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isSafeInteger(managerId) || managerId <= 0) {
      this.errorMessage = 'The requested travel manager could not be found.';
      this.loading = false;
      return;
    }
    let remaining = 4;
    const completeRequest = (): void => {
      remaining -= 1;
      this.loading = remaining > 0;
    };
    this.userService.getPublicManagerProfile(managerId).subscribe({
      next: (manager) => { this.manager = manager; completeRequest(); },
      error: (error: unknown) => { this.errorMessage = getApiErrorMessage(error); completeRequest(); }
    });
    this.bookingService.getPublicManagerStatistics(managerId).subscribe({
      next: (statistics) => { this.statistics = statistics; completeRequest(); },
      error: (error: unknown) => { this.errorMessage = getApiErrorMessage(error); completeRequest(); }
    });
    this.bookingService.getPublicManagerFeedback(managerId).subscribe({
      next: (feedback) => { this.feedback = feedback; completeRequest(); },
      error: (error: unknown) => { this.errorMessage = getApiErrorMessage(error); completeRequest(); }
    });
    this.travelService.getPublicManagerTravels(managerId).subscribe({
      next: (travels) => { this.travels = travels; completeRequest(); },
      error: (error: unknown) => { this.errorMessage = getApiErrorMessage(error); completeRequest(); }
    });
  }
}
