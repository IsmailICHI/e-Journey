import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NavbarComponent } from '../../shared/navbar/navbar.component';
import { getApiErrorMessage } from '../../core/interceptors/api-error';
import { BookingResponse } from '../../core/models/booking.model';
import { BookingService } from '../../core/services/booking.service';
import { PaymentOrderResponse, PaymentService } from '../../core/services/payment.service';

@Component({
  selector: 'app-payment',
  standalone: true,
  imports: [CommonModule, NavbarComponent, RouterLink],
  templateUrl: './payment.component.html'
})
export class PaymentComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly bookingService = inject(BookingService);
  private readonly paymentService = inject(PaymentService);

  booking: BookingResponse | null = null;
  order: PaymentOrderResponse | null = null;
  loading = true;
  preparingOrder = false;
  capturingPayment = false;
  paypalApprovalReturned = false;
  canRestartPayPalCheckout = false;
  errorMessage = '';
  statusMessage = '';

  ngOnInit(): void {
    const bookingId = Number(this.route.snapshot.paramMap.get('bookingId'));
    if (!Number.isSafeInteger(bookingId) || bookingId <= 0) {
      this.errorMessage = 'This payment link is invalid.';
      this.loading = false;
      return;
    }

    const sessionId = this.route.snapshot.queryParamMap.get('stripe_session_id');
    const paypalOrderId = this.route.snapshot.queryParamMap.get('token');
    const paymentCancelled = this.route.snapshot.queryParamMap.get('payment_cancelled') === 'true';
    this.bookingService.getBooking(bookingId).subscribe({
      next: (booking) => {
        this.booking = booking;
        this.loading = false;
        if (sessionId) {
          this.completeStripePayment(sessionId);
        } else if (paypalOrderId) {
          this.statusMessage = 'PayPal approved your payment. Confirming it now…';
          this.completePayPalReturn(paypalOrderId);
        } else {
          if (paymentCancelled) {
            this.statusMessage = 'Checkout was cancelled. Preparing a fresh checkout…';
          }
          this.preparePayment(paymentCancelled);
        }
      },
      error: (error: unknown) => {
        this.errorMessage = getApiErrorMessage(error);
        this.loading = false;
      }
    });
  }

  preparePayment(restart = false): void {
    if (!this.booking || this.booking.status !== 'PENDING_PAYMENT' || this.preparingOrder) {
      return;
    }
    this.preparingOrder = true;
    this.paymentService.createOrder(this.booking.id, restart).subscribe({
      next: (order) => {
        this.order = order;
        this.preparingOrder = false;
        this.errorMessage = '';
        if (order.provider === 'PAYPAL' && order.status === 'COMPLETED') {
          this.paypalApprovalReturned = true;
          this.capturePayPalPayment();
        }
        if (restart) {
          this.clearPaymentQueryParams();
          this.statusMessage = 'A new checkout is ready. Continue to PayPal to approve it.';
        }
      },
      error: (error: unknown) => {
        this.errorMessage = getApiErrorMessage(error);
        this.preparingOrder = false;
      }
    });
  }

  restartPayPalCheckout(): void {
    if (this.order?.provider !== 'PAYPAL' || !this.booking) {
      return;
    }
    this.errorMessage = '';
    this.statusMessage = '';
    this.paypalApprovalReturned = false;
    this.canRestartPayPalCheckout = false;
    this.preparePayment(true);
  }

  capturePayPalPayment(): void {
    if (!this.order || this.order.provider !== 'PAYPAL' || this.capturingPayment) {
      return;
    }
    this.capturingPayment = true;
    this.paymentService.capture(this.order.paymentId).subscribe({
      next: (order) => {
        this.order = order;
        this.statusMessage = 'PayPal confirmed your payment.';
        this.capturingPayment = false;
        this.paypalApprovalReturned = false;
        this.canRestartPayPalCheckout = false;
        this.reloadBooking();
      },
      error: (error: unknown) => {
        this.canRestartPayPalCheckout = error instanceof HttpErrorResponse && error.status === 409;
        this.errorMessage = error instanceof HttpErrorResponse && error.status === 409
          ? 'PayPal has not confirmed your approval yet. Complete the PayPal approval step, then return here. If checkout is stuck, restart it below.'
          : getApiErrorMessage(error);
        this.statusMessage = '';
        this.capturingPayment = false;
      }
    });
  }

  private completePayPalReturn(paypalOrderId: string): void {
    if (!this.booking) {
      return;
    }
    this.paymentService.createOrder(this.booking.id).subscribe({
      next: (order) => {
        this.order = order;
        if (order.provider !== 'PAYPAL' || order.providerOrderId !== paypalOrderId) {
          this.errorMessage = 'The returned PayPal order does not match this booking. Reopen checkout and try again.';
          this.statusMessage = '';
          this.clearPaymentQueryParams();
          return;
        }
        this.paypalApprovalReturned = true;
        this.capturePayPalPayment();
        this.clearPaymentQueryParams();
      },
      error: (error: unknown) => {
        this.errorMessage = getApiErrorMessage(error);
        this.statusMessage = '';
      }
    });
  }

  private completeStripePayment(sessionId: string): void {
    this.paymentService.completeStripeSession(sessionId).subscribe({
      next: (order) => {
        this.order = order;
        this.statusMessage = 'Stripe confirmed your payment. Your booking is complete.';
        this.clearPaymentQueryParams();
        this.reloadBooking();
      },
      error: (error: unknown) => {
        this.errorMessage = getApiErrorMessage(error);
        this.clearPaymentQueryParams();
      }
    });
  }

  private reloadBooking(): void {
    if (!this.booking) {
      return;
    }
    this.bookingService.getBooking(this.booking.id).subscribe({
      next: (booking) => { this.booking = booking; },
      error: (error: unknown) => { this.errorMessage = getApiErrorMessage(error); }
    });
  }

  private clearPaymentQueryParams(): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        stripe_session_id: null,
        paypal_return: null,
        token: null,
        PayerID: null,
        payment_cancelled: null
      },
      queryParamsHandling: 'merge',
      replaceUrl: true
    });
  }
}
