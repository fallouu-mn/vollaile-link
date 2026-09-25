export interface ClientDTO {
  id?: number;
  nom: string;
  prenom: string;
  telephone: string;
  email?: string;
  adresse?: string;
  ville?: string;
  region?: string;
  dateNaissance?: string; // ISO date string
  sexe?: string;
  professionnel: boolean;
  entreprise?: string;
  secteurActif?: string;
  isActive?: boolean;
  createdAt?: string;
  updatedAt?: string;
}