import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  BookingResponse,
  FeedbackResponse,
  ManagerPublicStatisticsResponse,
  ReportResponse,
  TravelerStatisticsResponse
} from '../models/booking.model';

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

  getMyStatistics(): Observable<TravelerStatisticsResponse> {
    return this.http.get<TravelerStatisticsResponse>(`${this.bookingsUrl}/me/statistics`);
  }

  getMyReports(): Observable<ReportResponse[]> {
    return this.http.get<ReportResponse[]>(`${this.bookingsUrl}/me/reports`);
  }

  cancelMySubscription(bookingId: number): Observable<void> {
    return this.http.delete<void>(`${this.bookingsUrl}/me/${bookingId}`);
  }

  getPublicManagerStatistics(managerId: number): Observable<ManagerPublicStatisticsResponse> {
    return this.http.get<ManagerPublicStatisticsResponse>(
      `${this.bookingsUrl}/public/managers/${managerId}/statistics`);
  }

  getPublicManagerFeedback(managerId: number): Observable<FeedbackResponse[]> {
    return this.http.get<FeedbackResponse[]>(
      `${this.bookingsUrl}/public/managers/${managerId}/feedback`);
  }

  getAllBookings(): Observable<BookingResponse[]> {
    return this.http.get<BookingResponse[]>(`${this.bookingsUrl}/admin/history`);
  }

  getManagerBookings(): Observable<BookingResponse[]> {
    return this.http.get<BookingResponse[]>(`${this.bookingsUrl}/manager/history`);
  }

  unsubscribeTraveler(bookingId: number): Observable<void> {
    return this.http.delete<void>(`${this.bookingsUrl}/manager/bookings/${bookingId}`);
  }

  submitFeedback(bookingId: number, rating: number, comment: string): Observable<FeedbackResponse> {
    return this.http.post<FeedbackResponse>(`${this.bookingsUrl}/feedback`, { bookingId, rating, comment });
  }

  getFeedback(): Observable<FeedbackResponse[]> {
    return this.http.get<FeedbackResponse[]>(`${this.bookingsUrl}/admin/feedback`);
  }

  getManagerFeedback(): Observable<FeedbackResponse[]> {
    return this.http.get<FeedbackResponse[]>(`${this.bookingsUrl}/manager/feedback`);
  }

  submitReport(targetType: 'TRAVEL' | 'MANAGER' | 'TRAVELER', targetId: number, reason: string, details: string): Observable<ReportResponse> {
    return this.http.post<ReportResponse>(`${this.bookingsUrl}/reports`, { targetType, targetId, reason, details });
  }

  getReports(): Observable<ReportResponse[]> {
    return this.http.get<ReportResponse[]>(`${this.bookingsUrl}/admin/reports`);
  }

  updateReportStatus(id: number, status: 'OPEN' | 'CLOSED'): Observable<ReportResponse> {
    return this.http.patch<ReportResponse>(`${this.bookingsUrl}/admin/reports/${id}`, { status });
  }
}
