import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { OfferStockDTO } from '../dto/offer-stock.dto';
import { PriceHistoryDTO } from '../dto/price-history.dto';

export type OfferStatus = 'DRAFT' | 'ACTIVE' | 'EXPIRED' | 'WITHDRAWN';

/** Offre et son état de stock, tel que renvoyé par /api/admin/stock/offres. */
export interface AdminOfferDTO extends OfferStockDTO {
  unitPrice?: number;
  status?: OfferStatus;
  producerId?: number;
  startsAt?: string;
  expiresAt?: string;
  isNegotiable?: boolean;
  description?: string;
}

@Injectable({
  providedIn: 'root'
})
export class OfferService {
  private apiUrl = '/api/admin';

  constructor(private http: HttpClient) {}

  /**
   * Liste des offres et de leur stock.
   * L'API d'état de stock (/stock/offres) est la source des compteurs ;
   * le prix et le statut sont lus sur la même réponse lorsqu'ils sont présents.
   */
  getOffers(): Observable<AdminOfferDTO[]> {
    return this.http.get<AdminOfferDTO[]>(`${this.apiUrl}/stock/offres`);
  }

  getOfferStock(offerId: number): Observable<OfferStockDTO> {
    return this.http.get<OfferStockDTO>(`${this.apiUrl}/stock/offres/${offerId}`);
  }

  /** Toute variation de prix est historisée et auditée côté serveur. */
  updatePrice(offerId: number, prix: number, motif: string): Observable<OfferStockDTO> {
    return this.http.patch<OfferStockDTO>(`${this.apiUrl}/offres/${offerId}/prix`, { prix, motif });
  }

  updateStatus(offerId: number, statut: OfferStatus, motif?: string): Observable<OfferStockDTO> {
    return this.http.patch<OfferStockDTO>(`${this.apiUrl}/offres/${offerId}/statut`, { statut, motif });
  }

  getPriceHistory(offerId: number): Observable<PriceHistoryDTO[]> {
    return this.http.get<PriceHistoryDTO[]>(`${this.apiUrl}/offres/${offerId}/prix`);
  }
}
