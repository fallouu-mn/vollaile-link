import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface DashboardStat {
  valeur: number;
  evolution: number | null;
  libelleEvolution: string | null;
}

export interface DashboardStock {
  total: number;
  reserve: number;
  vendu: number;
  disponible: number;
}

export interface DashboardActivity {
  icone: string;
  couleur: string;
  texte: string;
  type: string;
  horodatage: string;
}

export interface Dashboard {
  clientsActifs: DashboardStat;
  demandesEnCours: DashboardStat;
  commandesEnAttente: DashboardStat;
  stockDisponible: DashboardStat;
  reservationsActives: DashboardStat;
  producteursActifs: DashboardStat;
  offresActives: DashboardStat;
  notificationsNonLues: DashboardStat;
  stock: DashboardStock;
  activitesRecentes: DashboardActivity[];
}

/**
 * Données du tableau de bord.
 * Toutes les valeurs viennent du backend : aucun chiffre codé en dur,
 * qui donnerait une fausse impression d'activité.
 */
@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private apiUrl = '/api/admin/dashboard';

  constructor(private http: HttpClient) {}

  getDashboard(): Observable<Dashboard> {
    return this.http.get<Dashboard>(this.apiUrl);
  }
}
