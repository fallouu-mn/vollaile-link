/** Cycle de vie d'une réservation de stock. */
export type ReservationStatut = 'ACTIVE' | 'RELEASED' | 'CONSUMED' | 'EXPIRED';

export interface StockReservationDTO {
  id: number;
  offerId: number;
  commandeId?: number;
  commandeReference?: string;
  quantite: number;
  statut: ReservationStatut;
  /** Échéance calculée par le serveur : ne pas recalculer côté client. */
  expiresAt?: string;
  createdAt?: string;
  closedAt?: string;
  closeReason?: string;
}
