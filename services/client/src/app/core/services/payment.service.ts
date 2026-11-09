import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PaymentAnalyticsResponse } from '../models/booking.model';

export interface PaymentOrderResponse {
  paymentId: number;
  bookingId: number;
  travelId: number | null;
  managerId: number | null;
  amount: number;
  currency: string;
  status: string;
  approvalUrl: string;
}

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private readonly http = inject(HttpClient);
  private readonly paymentsUrl = `${environment.apiUrl}/api/v1/payments`;

  createOrder(bookingId: number): Observable<PaymentOrderResponse> {
    return this.http.post<PaymentOrderResponse>(`${this.paymentsUrl}/orders`, { bookingId });
  }

  capture(paymentId: number): Observable<PaymentOrderResponse> {
    return this.http.post<PaymentOrderResponse>(`${this.paymentsUrl}/${paymentId}/capture`, {});
  }

  getAdminAnalytics(months = 3): Observable<PaymentAnalyticsResponse> {
    return this.http.get<PaymentAnalyticsResponse>(`${environment.apiUrl}/api/v1/admin/payments/analytics`, {
      params: { months }
    });
  }

  getAdminPaymentHistory(): Observable<PaymentOrderResponse[]> {
    return this.http.get<PaymentOrderResponse[]>(`${environment.apiUrl}/api/v1/admin/payments/history`);
  }
}
