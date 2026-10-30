import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from '../services/auth.service';

export function roleGuard(allowedRoles: Array<'ADMIN' | 'TRAVEL_MANAGER' | 'TRAVELER' | 'USER'>): CanActivateFn {
  return (route, state) => {
    const router = inject(Router);
    const authService = inject(AuthService);

    if (!authService.isLoggedIn) {
      return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
    }

    return authService.verifySession().pipe(
      map((profile) => {
        const role = (profile.role ?? '').toUpperCase().replace(/^ROLE_/, '');
        if (allowedRoles.includes(role as 'ADMIN' | 'TRAVEL_MANAGER' | 'TRAVELER' | 'USER')) {
          return true;
        }
        return router.createUrlTree(['/error', 403]);
      }),
      catchError((error: unknown) => {
        if (!authService.isLoggedIn || (error instanceof HttpErrorResponse && error.status === 401)) {
          return of(router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } }));
        }
        const status = error instanceof HttpErrorResponse && error.status > 0 ? error.status : 503;
        return of(router.createUrlTree(['/error', status]));
      })
    );
  };
}
