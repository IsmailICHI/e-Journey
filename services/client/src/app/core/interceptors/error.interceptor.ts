import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const errorInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const requestPath = request.url.split('?')[0];
  const isAuthRequest = requestPath.endsWith('/api/v1/auth/login')
    || requestPath.endsWith('/api/v1/auth/register');
  const isSessionVerification = requestPath.endsWith('/api/v1/users/me');

  return next(request).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && !isAuthRequest) {
        if (error.status === 401) {
          auth.logout();
        }
        if (error.status >= 400 && (!isSessionVerification || error.status !== 401)) {
          void router.navigate(['/error', error.status]);
        } else if (error.status === 0) {
          void router.navigate(['/error', 503]);
        }
      }
      return throwError(() => error);
    })
  );
};
