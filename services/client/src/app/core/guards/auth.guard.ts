import { inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = (_route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  if (!authService.isLoggedIn) {
    return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
  }
  return authService.verifySession().pipe(
    map(() => true),
    catchError((error: unknown) => {
      if (!authService.isLoggedIn || (error instanceof HttpErrorResponse && error.status === 401)) {
        return of(router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } }));
      }
      const status = error instanceof HttpErrorResponse && error.status > 0 ? error.status : 503;
      return of(router.createUrlTree(['/error', status]));
    })
  );
};
