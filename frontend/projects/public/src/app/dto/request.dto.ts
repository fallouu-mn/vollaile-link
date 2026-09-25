export interface RequestDTO {
  nom: string;
  prenom: string;
  telephone: string;
  email?: string;
  typeClient: string;
  zoneLivraison: string;
  produitId?: number;
  nomProduit: string;
  quantiteSouhaitee: number;
  poidsSouhaite?: number;
  prixUnitaireSouhaite?: number;
  dateSouhaitee: string;
  adresseLivraison?: string;
  preferenceContact?: string;
  message?: string;
  offreId?: number;
  consentement: boolean;
}

export interface PublicDemandeAcknowledgementDTO {
  reference: string;
  message: string;
}