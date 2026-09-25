/**
 * Produit du catalogue avec son stock agrégé.
 *
 * Le stock est porté par les OFFRES, pas par le produit : le backend expose
 * donc une répartition explicite (total / réservé / vendu / disponible)
 * plutôt qu'un « stock » unique qui masquerait la règle du CDC.
 */
export interface ProductDTO {
  id?: number;
  name: string;
  description?: string;
  sku?: string;
  unitType?: string;
  categoryId?: number;
  categoryNom?: string;
  /** Prix indicatif catalogue. Le prix de vente réel est porté par l'offre. */
  defaultUnitPrice?: number;
  isActive?: boolean;
  createdAt?: string;
  updatedAt?: string;
  /** Nombre d'offres actives commercialisant ce produit. */
  activeOfferCount: number;
  stock: ProductStock;
}

export interface ProductStock {
  total: number;
  reserved: number;
  sold: number;
  /** total - reserved - sold */
  available: number;
}
