/**
 * État du stock d'une offre.
 * Règle du CDC : disponible = total - réservé - vendu.
 * L'offre est l'unité de stock : deux offres du même produit (producteurs
 * différents) n'ont pas la même disponibilité.
 */
export interface OfferStockDTO {
  offerId: number;
  productId?: number;
  productNom?: string;
  producerId?: number;
  quantityTotal: number;
  quantityReserved: number;
  quantitySold: number;
  quantityAvailable: number;
  activeReservationCount: number;
  computedAt?: string;
  /** Prix de vente de l'offre, en FCFA. */
  unitPrice?: number;
  status?: 'DRAFT' | 'ACTIVE' | 'EXPIRED' | 'WITHDRAWN';
  startsAt?: string;
  expiresAt?: string;
  isNegotiable?: boolean;
  description?: string;
}
