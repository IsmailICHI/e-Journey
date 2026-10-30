import { CanActivateFn } from '@angular/router';
import { roleGuard } from './role.guard';

export const travelManagerGuard: CanActivateFn = roleGuard(['ADMIN', 'TRAVEL_MANAGER']);
