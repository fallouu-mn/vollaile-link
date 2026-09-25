export interface CommandeDTO {
  id?: number;
  demandeId?: number;
  clientId: number;
  clientNom?: string;
  clientPrenom?: string;
  clientTelephone?: string;
  produitId: number;
  produitNom?: string;
  /**
   * Offre commandée : c'est elle qui porte le stock.
   * Deux offres du même produit (producteurs différents) ont des stocks distincts.
   */
  offreId?: number;
  quantite: number;
  prixUnitaire: number;
  prixUnitaireAchat?: number;
  remisePourcentage: number;
  remiseMontant: number;
  /** Le backend sérialise en camelCase : totalHt / totalTtc (et non totalHT). */
  totalHt: number;
  totalTtc: number;
  /** EN_ATTENTE_PAIEMENT, PAYEE, EN_PREPARATION, EXPEDIEE, LIVREE, ANNULEE, EXPIREE */
  statut: string;
  modePaiement?: string;
  datePaiement?: string; // ISO date string
  dateLivraisonPrev: string; // ISO date string
  dateLivraisonReelle?: string; // ISO date string
  lieuLivraison: string;
  numeroSuivi?: string;
  commentaires?: string;
  isActive?: boolean;
  createdAt?: string;
  updatedAt?: string;
}
