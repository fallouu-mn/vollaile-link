import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatIconModule } from '@angular/material/icon';
import { DashboardService, Dashboard, DashboardActivity } from '../services/dashboard.service';

@Component({
  selector: 'app-dashboard',
  standalone: false,
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {

  isLoading = true;
  errorMessage: string | null = null;

  // Regroupés pour que le template reste lisible, tout en conservant
  // les noms de champs attendus par le template existant.
  clientsActifs = { valeur: 0, evolution: null as number | null, libelleEvolution: null as string | null };
  demandesEnCours = { valeur: 0, evolution: null as number | null, libelleEvolution: null as string | null };
  commandesEnAttente = { valeur: 0, evolution: null as number | null, libelleEvolution: null as string | null };
  stockDisponible = { valeur: 0, evolution: null as number | null, libelleEvolution: null as string | null };
  reservationsActives = { valeur: 0, evolution: null as number | null, libelleEvolution: null as string | null };
  producteursActifs = { valeur: 0, evolution: null as number | null, libelleEvolution: null as string | null };
  offresActives = { valeur: 0, evolution: null as number | null, libelleEvolution: null as string | null };
  notificationsNonLues = { valeur: 0, evolution: null as number | null, libelleEvolution: null as string | null };

  stock = { total: 0, reserve: 0, vendu: 0, disponible: 0 };
  recentActivities: DashboardActivity[] = [];

  constructor(
    private dashboardService: DashboardService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.isLoading = true;
    this.errorMessage = null;

    this.dashboardService.getDashboard().subscribe({
      next: (data: Dashboard) => {
        this.clientsActifs = data.clientsActifs;
        this.demandesEnCours = data.demandesEnCours;
        this.commandesEnAttente = data.commandesEnAttente;
        this.stockDisponible = data.stockDisponible;
        this.reservationsActives = data.reservationsActives;
        this.producteursActifs = data.producteursActifs;
        this.offresActives = data.offresActives;
        this.notificationsNonLues = data.notificationsNonLues;
        this.stock = data.stock;
        this.recentActivities = data.activitesRecentes ?? [];
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.error('Error loading dashboard:', error);
        this.errorMessage = 'Impossible de charger le tableau de bord.';
        this.isLoading = false;
        this.cdr.markForCheck();
      }
    });
  }

  /** "il y a 2 h", "hier", ou la date courte. */
  formatRelativeDate(iso: string | null | undefined): string {
    if (!iso) return '—';

    const date = new Date(iso);
    const diffMinutes = Math.floor((Date.now() - date.getTime()) / 60000);

    if (diffMinutes < 1) return "à l'instant";
    if (diffMinutes < 60) return `il y a ${diffMinutes} min`;

    const diffHeures = Math.floor(diffMinutes / 60);
    if (diffHeures < 24) return `il y a ${diffHeures} h`;

    const diffJours = Math.floor(diffHeures / 24);
    if (diffJours === 1) return 'hier';
    if (diffJours < 7) return `il y a ${diffJours} jours`;

    return date.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' });
  }
}
