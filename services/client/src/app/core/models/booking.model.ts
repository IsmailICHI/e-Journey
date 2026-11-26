export interface BookingResponse {
  id: number;
  travelId: number;
  userId: number;
  travelTitle: string | null;
  paymentMethod: string | null;
  departureDate: string | null;
  priceMad: number | null;
  managerId: number | null;
  status: string;
  feedbackSubmitted: boolean;
  createdAt: string;
}

export interface FeedbackResponse {
  id: number;
  bookingId: number;
  travelerId: number;
  travelId: number;
  managerId: number | null;
  rating: number;
  comment: string;
  createdAt: string;
}

export interface ReportResponse {
  id: number;
  reporterId: number;
  targetType: 'TRAVEL' | 'MANAGER' | 'TRAVELER';
  targetId: number;
  reason: string;
  details: string;
  status: 'OPEN' | 'CLOSED';
  createdAt: string;
  closedAt: string | null;
}

export interface TravelerStatisticsResponse {
  pastTrips: number;
  reportsFiled: number;
  cancellations: number;
  preferredPaymentMethod: string | null;
}

export interface ManagerPublicStatisticsResponse {
  bookings: number;
  feedbackCount: number;
  averageRating: number | null;
  reportCount: number;
}

export interface PaymentAnalyticsResponse {
  months: number;
  currency: string;
  totalIncome: number;
  monthlyIncome: Array<{ month: string; income: number; payments: number }>;
  topTravels: Array<{ travelId: number; title: string; income: number; payments: number }>;
  managerIncome: Array<{ managerId: number; income: number; payments: number }>;
}
