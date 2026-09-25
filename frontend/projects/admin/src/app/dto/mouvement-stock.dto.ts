export interface MouvementStockDTO {
  id?: number;
  produitId: number;
  /** Offre concernée : le stock est porté par l'offre, pas par le produit. */
  offerId?: number;
  /** Réservation à l'origine du mouvement, si elle existe. */
  reservationId?: number;
  typeMouvement: string; // ENTREE, SORTIE, RESERVATION, LIBERATION_RESERVATION, VENTE, AJUSTEMENT, INVENTAIRE
  quantite: number;
  /** Stock disponible de l'offre après le mouvement. */
  quantiteApresMouvement: number;
  reference: string;
  referenceType: string; // DEMANDE, COMMANDE, INVENTAIRE, AJUSTEMENT
  motif: string;
  isActive?: boolean;
  createdAt?: string; // ISO date string
  updatedAt?: string; // ISO date string
  createdBy?: number;
}
