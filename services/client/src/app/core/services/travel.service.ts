import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../models/page.model';
import { TravelRequest, TravelResponse } from '../models/travel.model';

@Injectable({ providedIn: 'root' })
export class TravelService {
  private readonly http = inject(HttpClient);
  private readonly travelsUrl = `${environment.apiUrl}/api/v1/admin/travels`;

  getTravels(page = 0): Observable<PageResponse<TravelResponse>> {
    return this.http.get<PageResponse<TravelResponse>>(this.travelsUrl, { params: { page } });
  }

  createTravel(travel: TravelRequest): Observable<TravelResponse> {
    return this.http.post<TravelResponse>(this.travelsUrl, travel);
  }

  updateTravel(id: number, travel: TravelRequest): Observable<TravelResponse> {
    return this.http.put<TravelResponse>(`${this.travelsUrl}/${id}`, travel);
  }

  deleteTravel(id: number): Observable<void> {
    return this.http.delete<void>(`${this.travelsUrl}/${id}`);
  }
}
