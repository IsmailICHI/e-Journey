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
          if (!isSessionVerification) {
            void router.navigate(['/login']);
          }
        }
        // Never navigate away on 400 Bad Request, 403, 404, 409, 422, 429
        // Allow the component and feedback dialog system to show rich in-place feedback!
        if ((error.status === 0 || error.status === 503) && request.method === 'GET' && !isSessionVerification) {
          void router.navigate(['/error', 503]);
        }
      }
      return throwError(() => error);
    })
  );
};
