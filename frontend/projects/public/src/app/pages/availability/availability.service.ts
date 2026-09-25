import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface OfferPublicDTO {
  id: number;
  productName: string;
  productDescription?: string;
  unitType?: string;
  unitPrice: number;
  quantityAvailable: number;
  totalValue?: number;
  status?: string;
  startsAt?: string;
  expiresAt?: string;
  description?: string;
}

export interface OfferFilters {
  productName?: string;
  minQuantity?: number;
  maxQuantity?: number;
  minPrice?: number;
  maxPrice?: number;
}

@Injectable({
  providedIn: 'root'
})
export class AvailabilityService {

  private apiUrl = '/api/public/offers';

  constructor(private http: HttpClient) { }

  getOffers(filters: OfferFilters = {}): Observable<OfferPublicDTO[]> {
    // Build query parameters
    let params = new URLSearchParams();

    if (filters.productName) {
      params.set('productName', filters.productName);
    }
    if (filters.minQuantity !== null && filters.minQuantity !== undefined) {
      params.set('minQuantity', filters.minQuantity.toString());
    }
    if (filters.maxQuantity !== null && filters.maxQuantity !== undefined) {
      params.set('maxQuantity', filters.maxQuantity.toString());
    }
    if (filters.minPrice !== null && filters.minPrice !== undefined) {
      params.set('minPrice', filters.minPrice.toString());
    }
    if (filters.maxPrice !== null && filters.maxPrice !== undefined) {
      params.set('maxPrice', filters.maxPrice.toString());
    }

    const queryString = params.toString();
    const url = queryString ? `${this.apiUrl}?${queryString}` : this.apiUrl;

    return this.http.get<OfferPublicDTO[]>(url);
  }
}