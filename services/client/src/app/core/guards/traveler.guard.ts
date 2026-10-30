import { CanActivateFn } from '@angular/router';
import { roleGuard } from './role.guard';

export const travelerGuard: CanActivateFn = roleGuard(['ADMIN', 'TRAVEL_MANAGER', 'TRAVELER', 'USER']);
