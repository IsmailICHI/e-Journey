import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { BookingResponse, FeedbackResponse, ReportResponse } from '../models/booking.model';

@Injectable({ providedIn: 'root' })
export class BookingService {
  private readonly http = inject(HttpClient);
  private readonly bookingsUrl = `${environment.apiUrl}/api/v1/bookings`;

  createBooking(travelId: number): Observable<BookingResponse> {
    return this.http.post<BookingResponse>(this.bookingsUrl, { travelId });
  }

  getMyBookings(): Observable<BookingResponse[]> {
    return this.http.get<BookingResponse[]>(`${this.bookingsUrl}/me`);
  }

  getAllBookings(): Observable<BookingResponse[]> {
    return this.http.get<BookingResponse[]>(`${this.bookingsUrl}/admin/history`);
  }

  submitFeedback(bookingId: number, rating: number, comment: string): Observable<FeedbackResponse> {
    return this.http.post<FeedbackResponse>(`${this.bookingsUrl}/feedback`, { bookingId, rating, comment });
  }

  getFeedback(): Observable<FeedbackResponse[]> {
    return this.http.get<FeedbackResponse[]>(`${this.bookingsUrl}/admin/feedback`);
  }

  submitReport(targetType: 'TRAVEL' | 'MANAGER', targetId: number, reason: string, details: string): Observable<ReportResponse> {
    return this.http.post<ReportResponse>(`${this.bookingsUrl}/reports`, { targetType, targetId, reason, details });
  }

  getReports(): Observable<ReportResponse[]> {
    return this.http.get<ReportResponse[]>(`${this.bookingsUrl}/admin/reports`);
  }

  updateReportStatus(id: number, status: 'OPEN' | 'CLOSED'): Observable<ReportResponse> {
    return this.http.patch<ReportResponse>(`${this.bookingsUrl}/admin/reports/${id}`, { status });
  }
}
