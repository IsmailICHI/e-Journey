import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { TravelResponse } from '../models/travel.model';
import { TravelService } from './travel.service';
import { UserService } from './user.service';

describe('Admin API services', () => {
  let users: UserService;
  let travels: TravelService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    users = TestBed.inject(UserService);
    travels = TestBed.inject(TravelService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists users and deletes a user by the backend DTO id', () => {
    let userCount = 0;
    users.getUsers().subscribe((result) => userCount = result.content.length);
    http.expectOne(`${environment.apiUrl}/api/v1/admin/users?page=0`)
      .flush({
        content: [{ id: 8, email: 'person@example.test', fullName: 'Traveler', role: 'User', state: true }],
        page: 0, size: 5, totalElements: 1, totalPages: 1, last: true
      });
    expect(userCount).toBe(1);

    let deleted = false;
    users.deleteUser(8).subscribe(() => deleted = true);
    const request = http.expectOne(`${environment.apiUrl}/api/v1/admin/users/8`);
    expect(request.request.method).toBe('DELETE');
    request.flush(null);
    expect(deleted).toBeTrue();
  });

  it('uses the admin travel endpoint for listing, creating, updating, and deleting', () => {
    const travel: TravelResponse = {
      travelId: 3,
      title: 'Coastal trip',
      city: 'Essaouira',
      country: 'Morocco',
      description: null,
      imageUrl: null,
      duration: 4,
      destinations: ['Sidi Kaouki'],
      startDate: null,
      endDate: null,
      accommodation: null,
      transportation: null,
      paymentMethod: 'Stripe',
      itinerary: []
    };
    const apiUrl = `${environment.apiUrl}/api/v1/admin/travels`;

    travels.getTravels().subscribe();
    http.expectOne(`${apiUrl}?page=0`).flush({
      content: [travel], page: 0, size: 5, totalElements: 1, totalPages: 1, last: true
    });

    travels.createTravel({
      title: travel.title, city: travel.city, country: travel.country, description: '',
      imageUrl: '', duration: 4, destinations: ['Sidi Kaouki'], startDate: null, endDate: null,
      accommodation: '', transportation: '', paymentMethod: 'Stripe', itinerary: []
    }).subscribe();
    const createRequest = http.expectOne(apiUrl);
    expect(createRequest.request.method).toBe('POST');
    createRequest.flush(travel);

    travels.updateTravel(3, {
      title: travel.title, city: travel.city, country: travel.country, description: '',
      imageUrl: '', duration: 4, destinations: [], startDate: null, endDate: null,
      accommodation: '', transportation: '', paymentMethod: 'PayPal', itinerary: []
    }).subscribe();
    const updateRequest = http.expectOne(`${apiUrl}/3`);
    expect(updateRequest.request.method).toBe('PUT');
    updateRequest.flush(travel);

    travels.deleteTravel(3).subscribe();
    const deleteRequest = http.expectOne(`${apiUrl}/3`);
    expect(deleteRequest.request.method).toBe('DELETE');
    deleteRequest.flush(null);
  });
});
