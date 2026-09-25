export interface DemandeDTO {
  id?: number;
  clientId: number;
  produitId?: number;
  quantiteSouhaitee?: number;
  prixUnitaireSouhaite?: number;
  nomProduit?: string;
  description?: string;
  statut?: string; // EN_COURS, VALIDEE, REFUSEE, EXPIREE, SANS_SUITE
  dateSouhaitee?: string; // ISO date string
  lieuLivraison?: string;
  commentairesAdmin?: string;
  isActive?: boolean;
  createdAt?: string;
  updatedAt?: string;
}