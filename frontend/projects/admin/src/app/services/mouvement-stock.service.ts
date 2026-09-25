import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { MouvementStockDTO } from '../dto/mouvement-stock.dto';
import { OfferStockDTO } from '../dto/offer-stock.dto';
import { StockReservationDTO } from '../dto/stock-reservation.dto';

/**
 * Client du back-office stock.
 *
 * Le stock est porté par l'offre et suit la règle du CDC :
 *   disponible = total - réservé - vendu
 *
 * Toutes les mutations passent par le backend, qui applique l'invariant
 * sous verrou pessimiste. Le frontend ne calcule jamais un stock.
 */
@Injectable({
  providedIn: 'root'
})
export class MouvementStockService {
  private apiUrl = '/api/admin/stock';

  constructor(private http: HttpClient) {}

  // --- Lecture du stock par offre ---

  getAllOfferStock(): Observable<OfferStockDTO[]> {
    return this.http.get<OfferStockDTO[]>(`${this.apiUrl}/offres`);
  }

  getOfferStock(offerId: number): Observable<OfferStockDTO> {
    return this.http.get<OfferStockDTO>(`${this.apiUrl}/offres/${offerId}`);
  }

  // --- Journal des mouvements ---

  getAllMouvements(offerId?: number, produitId?: number): Observable<MouvementStockDTO[]> {
    return this.http.get<MouvementStockDTO[]>(`${this.apiUrl}/mouvements`, {
      params: {
        ...(offerId ? { offerId: offerId.toString() } : {}),
        ...(produitId ? { produitId: produitId.toString() } : {})
      }
    });
  }

  getMouvementsByOffer(offerId: number): Observable<MouvementStockDTO[]> {
    return this.getAllMouvements(offerId);
  }

  getMouvementsByProduit(produitId: number): Observable<MouvementStockDTO[]> {
    return this.getAllMouvements(undefined, produitId);
  }

  // --- Réservations ---

  getActiveReservations(offerId?: number): Observable<StockReservationDTO[]> {
    return this.http.get<StockReservationDTO[]>(`${this.apiUrl}/reservations`, {
      params: offerId ? { offerId: offerId.toString() } : {}
    });
  }

  getReservation(reservationId: number): Observable<StockReservationDTO> {
    return this.http.get<StockReservationDTO>(`${this.apiUrl}/reservations/${reservationId}`);
  }

  /** Libération manuelle. Idempotent côté serveur. */
  releaseReservation(reservationId: number, motif?: string): Observable<StockReservationDTO> {
    return this.http.post<StockReservationDTO>(
      `${this.apiUrl}/reservations/${reservationId}/liberer`,
      { motif }
    );
  }

  /**
   * Déclenche le balayage des réservations échues.
   * Même traitement que le planificateur automatique.
   */
  expireOverdueReservations(): Observable<{ reservationsLiberees: number }> {
    return this.http.post<{ reservationsLiberees: number }>(
      `${this.apiUrl}/reservations/expire`,
      {}
    );
  }

  // --- Mouvements physiques de stock ---

  /** Entrée de marchandise : le total et le disponible augmentent. */
  entreeStock(offerId: number, quantite: number, motif?: string): Observable<OfferStockDTO> {
    return this.http.post<OfferStockDTO>(`${this.apiUrl}/offres/${offerId}/entree`, {
      quantite,
      motif
    });
  }

  /** Sortie de marchandise : le total baisse. */
  sortieStock(offerId: number, quantite: number, motif?: string): Observable<OfferStockDTO> {
    return this.http.post<OfferStockDTO>(`${this.apiUrl}/offres/${offerId}/sortie`, {
      quantite,
      motif
    });
  }

  /** Ajustement d'inventaire avec un delta signé (négatif = baisse). */
  ajusterStock(offerId: number, delta: number, motif?: string): Observable<OfferStockDTO> {
    return this.http.post<OfferStockDTO>(`${this.apiUrl}/offres/${offerId}/ajustement`, {
      quantite: delta,
      motif
    });
  }
}
