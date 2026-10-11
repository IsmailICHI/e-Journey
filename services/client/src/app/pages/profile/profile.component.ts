import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { forkJoin } from 'rxjs';
import { getApiErrorMessage } from '../../core/interceptors/api-error';
import { AuthService } from '../../core/services/auth.service';
import { TravelService } from '../../core/services/travel.service';
import { UserService } from '../../core/services/user.service';
import { NavbarComponent } from '../../shared/navbar/navbar.component';
import { UserProfileResponse } from '../../core/models/user.model';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, NavbarComponent],
  templateUrl: './profile.component.html'
})
export class ProfileComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly travelService = inject(TravelService);
  private readonly userService = inject(UserService);

  readonly email = this.authService.email;
  readonly role = this.authService.role;
  readonly isAdmin = this.authService.isAdmin;
  profile: UserProfileResponse | null = null;
  loadingProfile = true;
  userCount = 0;
  activeUserCount = 0;
  travelCount = 0;
  loadingStatistics = false;
  profileErrorMessage = '';
  statisticsErrorMessage = '';

  ngOnInit(): void {
    this.userService.getMyProfile().subscribe({
      next: (profile) => {
        this.profile = profile;
        this.loadingProfile = false;
      },
      error: (error: unknown) => {
        this.profileErrorMessage = getApiErrorMessage(error);
        this.loadingProfile = false;
      }
    });

    if (this.isAdmin) {
      this.loadingStatistics = true;
      forkJoin({
        users: this.userService.getUserStatistics(),
        travels: this.travelService.getTravels(0)
      }).subscribe({
        next: ({ users, travels }) => {
          this.userCount = users.totalUsers;
          this.activeUserCount = users.activeUsers;
          this.travelCount = travels.totalElements;
          this.loadingStatistics = false;
        },
        error: (error: unknown) => {
          this.statisticsErrorMessage = getApiErrorMessage(error);
          this.loadingStatistics = false;
        }
      });
    }
  }
}
