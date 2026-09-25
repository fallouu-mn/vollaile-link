import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AuditLogEntry {
  id: number;
  administratorId?: number;
  administratorPhone?: string;
  eventType: string;
  eventDescription?: string;
  ipAddress?: string;
  eventTimestamp: string;
  /** Blob JSON : détail technique de l'événement. */
  metadata?: string;
}

/** Types d'événements proposed dans le filtre. */
export const AUDIT_EVENT_TYPES: { value: string; label: string }[] = [
  { value: '', label: 'Tous les événements' },
  { value: 'STOCK_ENTREE', label: 'Entrée de stock' },
  { value: 'STOCK_SORTIE', label: 'Sortie de stock' },
  { value: 'STOCK_AJUSTEMENT', label: 'Ajustement de stock' },
  { value: 'STOCK_RESERVATION', label: 'Réservation' },
  { value: 'STOCK_LIBERATION', label: 'Libération de réservation' },
  { value: 'STOCK_VENTE', label: 'Vente' },
  { value: 'PRIX_OFFRE_CHANGEMENT', label: 'Prix d\'offre' },
  { value: 'PRIX_PRODUIT_CHANGEMENT', label: 'Prix produit' },
  { value: 'OFFRE_STATUT_CHANGEMENT', label: 'Statut d\'offre' },
  { value: 'COMMANDE_STATUT_CHANGEMENT', label: 'Statut de commande' },
  { value: 'DEMANDE_STATUT_CHANGEMENT', label: 'Statut de demande' },
  { value: 'ZONE_LIVRAISON_CREEE', label: 'Zone créée' },
  { value: 'ZONE_LIVRAISON_MODIFIEE', label: 'Zone modifiée' },
  { value: 'ZONE_LIVRAISON_SUPPRIMEE', label: 'Zone supprimée' },
  { value: 'LOGIN_SUCCESS', label: 'Connexion réussie' },
  { value: 'LOGIN_FAILURE', label: 'Échec de connexion' }
];

@Injectable({
  providedIn: 'root'
})
export class AuditService {
  private apiUrl = '/api/admin/audit';

  constructor(private http: HttpClient) {}

  getAuditLog(eventType?: string, page = 0, size = 50): Observable<AuditLogEntry[]> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (eventType) {
      params = params.set('eventType', eventType);
    }
    return this.http.get<AuditLogEntry[]>(this.apiUrl, { params });
  }

  getAuditLogByOffer(offerId: number, page = 0, size = 50): Observable<AuditLogEntry[]> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<AuditLogEntry[]>(`${this.apiUrl}/offres/${offerId}`, { params });
  }
}
