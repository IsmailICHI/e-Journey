import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { BookingResponse } from '../models/booking.model';

@Injectable({ providedIn: 'root' })
export class BookingService {
  private readonly http = inject(HttpClient);
  private readonly bookingsUrl = `${environment.apiUrl}/api/v1/bookings`;

  createBooking(travelId: number): Observable<BookingResponse> {
    return this.http.post<BookingResponse>(this.bookingsUrl, { travelId });
  }
}
