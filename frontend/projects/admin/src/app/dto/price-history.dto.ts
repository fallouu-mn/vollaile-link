/**
 * Variation de prix historisée.
 * `reason` porte le motif saisi par l'administrateur, obligatoire côté serveur
 * pour que la variation soit traçable (CDC §2.5).
 */
export interface PriceHistoryDTO {
  id: number;
  productId?: number;
  productNom?: string;
  price: number;
  changedBy?: number;
  changedByPhone?: string;
  changedAt?: string;
  reason?: string;
}
