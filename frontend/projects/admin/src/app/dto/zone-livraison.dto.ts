/**
 * Zone de livraison desservie.
 *
 * Les frais sont en FCFA (0 = livraison incluse) et le délai est exprimé
 * en heures, pas en texte libre : le backend contrôle la cohérence
 * delaiMin ≤ delaiMax.
 */
export interface ZoneLivraisonDTO {
  id?: number;
  nom: string;
  code: string;
  description?: string;
  fraisLivraison: number;
  delaiMinHeures: number;
  delaiMaxHeures: number;
  estActive: boolean;
  createdAt?: string;
  updatedAt?: string;
}
