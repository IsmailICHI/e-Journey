export interface ItineraryResponse {
  id: number;
  dayNumber: number;
  title: string;
  description: string | null;
  activities: string[];
}

export interface ItineraryRequest {
  dayNumber: number;
  title: string;
  description: string | null;
  activities: string[];
}

export interface TravelResponse {
  travelId: number;
  title: string;
  city: string;
  country: string;
  description: string | null;
  imageUrl: string | null;
  duration: number;
  destinations: string[];
  startDate: string | null;
  endDate: string | null;
  accommodation: string | null;
  transportation: string | null;
  paymentMethod: string | null;
  itinerary: ItineraryResponse[];
}

export interface TravelRequest {
  title: string;
  city: string;
  country: string;
  description: string;
  imageUrl: string;
  duration: number;
  destinations: string[];
  startDate: string | null;
  endDate: string | null;
  accommodation: string;
  transportation: string;
  paymentMethod: string;
  itinerary: ItineraryRequest[];
}
