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

@Injectable({
  providedIn: 'root'
})
export class OfferDetailService {

  private apiUrl = '/api/public/offers';

  constructor(private http: HttpClient) { }

  getOfferById(id: number): Observable<OfferPublicDTO> {
    return this.http.get<OfferPublicDTO>(`${this.apiUrl}/${id}`);
  }
}