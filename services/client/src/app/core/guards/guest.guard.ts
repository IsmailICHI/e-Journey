import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const guestGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  
  if (!authService.isLoggedIn) {
    return true;
  }
  
  return authService.verifySession().pipe(
    map(() => router.createUrlTree(['/home'])),
    catchError((error: unknown) => {
      if (!authService.isLoggedIn || (error instanceof HttpErrorResponse && error.status === 401)) {
        return of(true);
      }
      const status = error instanceof HttpErrorResponse && error.status > 0 ? error.status : 503;
      return of(router.createUrlTree(['/error', status]));
    })
  );
};