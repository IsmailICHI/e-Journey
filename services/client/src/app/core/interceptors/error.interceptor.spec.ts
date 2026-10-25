import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { errorInterceptor } from './error.interceptor';

describe('errorInterceptor', () => {
  let httpClient: HttpClient;
  let http: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    localStorage.setItem('e-journey.token', 'revoked-token');
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    });
    httpClient = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
    sessionStorage.clear();
  });

  it('clears the stored token and redirects to login on an unauthorized response', () => {
    let responseError: HttpErrorResponse | undefined;
    httpClient.get('/api/v1/travels').subscribe({ error: (error: HttpErrorResponse) => responseError = error });
    http.expectOne('/api/v1/travels').flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(responseError?.status).toBe(401);
    expect(localStorage.getItem('e-journey.token')).toBeNull();
    expect(sessionStorage.getItem('e-journey.token')).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });
});
