import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MouvementStockService } from '../../services/mouvement-stock.service';
import { ExportService } from '../../services/export.service';
import { StockReservationDTO, ReservationStatut } from '../../dto/stock-reservation.dto';
import { ProductService } from '../../services/product.service';
import { ProductDTO } from '../../dto/product.dto';
import { CommandeService } from '../../services/commande.service';
import { CommandeDTO } from '../../dto/commande.dto';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatTooltipModule } from '@angular/material/tooltip';

/**
 * Gestion des réservations de stock.
 *
 * Les données proviennent de la table réelle `stock_reservations`.
 * L'ancienne version devinait le statut d'une réservation en cherchant
 * un mouvement LIBERATION_RESERVATION correspondant, et recalculait
 * l'expiration côté client (20 minutes codées en dur) : les deux étaient
 * sources d'écart avec le serveur.
 */
@Component({
  selector: 'app-reservation-management',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatToolbarModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressBarModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatTooltipModule,
    MatSnackBarModule
  ],
  templateUrl: './reservation-management.component.html',
  styleUrls: ['./reservation-management.component.scss']
})
export class ReservationManagementComponent implements OnInit {
  reservations: StockReservationDTO[] = [];
  commandes: CommandeDTO[] = [];
  produits: ProductDTO[] = [];

  isLoading = false;
  isExporting = false;
  errorMessage: string | null = null;

  displayedColumns: string[] = [
    'id', 'offre', 'produitNom', 'commande', 'quantite', 'dateReservation', 'dateExpiration', 'statut', 'actions'
  ];
  dataSource!: MatTableDataSource<StockReservationDTO>;

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  private produitMap: Map<number, string> = new Map();
  private commandeMap: Map<number, CommandeDTO> = new Map();

  constructor(
    private mouvementStockService: MouvementStockService,
    private productService: ProductService,
    private commandeService: CommandeService,
    private exportService: ExportService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading = true;
    this.errorMessage = null;

    forkJoin({
      reservations: this.mouvementStockService.getActiveReservations(),
      produits: this.productService.getAllProducts(),
      commandes: this.commandeService.getAllCommandes()
    }).subscribe({
      next: ({ reservations, produits, commandes }) => {
        this.reservations = reservations;
        this.produits = produits;
        this.commandes = commandes;

        this.produitMap.clear();
        produits.forEach(produit => {
          if (produit.id && produit.name) {
            this.produitMap.set(produit.id, produit.name);
          }
        });

        this.commandeMap.clear();
        commandes.forEach(commande => {
          if (commande.id) {
            this.commandeMap.set(commande.id, commande);
          }
        });

        this.dataSource = new MatTableDataSource(this.reservations);
        this.dataSource.paginator = this.paginator;
        this.dataSource.sort = this.sort;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading reservations:', error);
        this.errorMessage = 'Impossible de charger les réservations.';
        this.isLoading = false;
      }
    });
  }

  applyFilter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();

    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }

  // --- Helpers d'affichage ---

  getOffreLabel(reservation: StockReservationDTO): string {
    return `Offre #${reservation.offerId}`;
  }

  /**
   * Le produit est déduit de la commande liée. La réservation porte l'offre,
   * qui est l'unité de stock : deux offres du même produit sont distinctes.
   */
  getProduitName(reservation: StockReservationDTO): string {
    const commande = reservation.commandeId ? this.commandeMap.get(reservation.commandeId) : undefined;
    if (commande?.produitNom) {
      return commande.produitNom;
    }
    return commande?.produitId ? this.produitMap.get(commande.produitId) ?? `Produit #${commande.produitId}` : '—';
  }

  getCommandeLabel(reservation: StockReservationDTO): string {
    if (!reservation.commandeId) {
      return '—';
    }
    return reservation.commandeReference ?? `Commande #${reservation.commandeId}`;
  }

  getReservationDate(reservation: StockReservationDTO): string {
    return reservation.createdAt ? new Date(reservation.createdAt).toLocaleString() : '—';
  }

  /**
   * Échéance fournie par le serveur. Ne jamais recalculer ici :
   * la durée de vie d'une réservation est une décision serveur.
   */
  getExpirationDate(reservation: StockReservationDTO): string {
    return reservation.expiresAt ? new Date(reservation.expiresAt).toLocaleString() : '—';
  }

  /** Vrai si l'échéance est dépassée, selon l'horodatage du serveur. */
  isExpired(reservation: StockReservationDTO): boolean {
    if (!reservation.expiresAt) {
      return false;
    }
    return new Date(reservation.expiresAt).getTime() < Date.now();
  }

  getStatutLabel(statut: ReservationStatut): string {
    const labels: Record<ReservationStatut, string> = {
      'ACTIVE': 'Active',
      'RELEASED': 'Libérée',
      'CONSUMED': 'Convertie en vente',
      'EXPIRED': 'Expirée'
    };
    return labels[statut] ?? statut;
  }

  getStatusColor(statut: ReservationStatut): string {
    const colors: Record<ReservationStatut, string> = {
      'ACTIVE': '#ff9800',
      'RELEASED': '#607d8b',
      'CONSUMED': '#2196f3',
      'EXPIRED': '#9c27b0'
    };
    return colors[statut] ?? '#9e9e9e';
  }

  // --- Actions ---

  /** Libération manuelle. Idempotent côté serveur. */
  cancelReservation(reservation: StockReservationDTO): void {
    if (!confirm(
      `Libérer la réservation #${reservation.id} de ${reservation.quantite} unité(s) ?\n` +
      'Le stock redeviendra immédiatement disponible.'
    )) {
      return;
    }

    this.mouvementStockService
      .releaseReservation(reservation.id, 'Libération manuelle depuis le back-office')
      .subscribe({
        next: () => {
          this.snackBar.open('Réservation libérée', 'Fermer', { duration: 4000 });
          this.loadData();
        },
        error: (error) => {
          console.error('Error releasing reservation:', error);
          this.snackBar.open('Erreur lors de la libération', 'Fermer', { duration: 5000 });
        }
      });
  }

  /** Export des réservations actives, journalisé côté serveur. */
  exportCsv(): void {
    this.isExporting = true;
    this.exportService.download('reservations').subscribe({
      next: () => {
        this.isExporting = false;
        this.snackBar.open('Export téléchargé et journalisé', 'Fermer', { duration: 4000 });
      },
      error: (error) => {
        console.error('Error exporting reservations:', error);
        this.isExporting = false;
        this.snackBar.open('Erreur lors de l\'export', 'Fermer', { duration: 5000 });
      }
    });
  }

  /** Balayage manuel, équivalent au planificateur automatique. */
  expireOverdue(): void {    this.mouvementStockService.expireOverdueReservations().subscribe({
      next: (result) => {
        this.snackBar.open(
          `${result.reservationsLiberees} réservation(s) libérée(s)`,
          'Fermer',
          { duration: 4000 }
        );
        this.loadData();
      },
      error: (error) => {
        console.error('Error expiring reservations:', error);
        this.snackBar.open('Erreur lors du balayage', 'Fermer', { duration: 5000 });
      }
    });
  }
}
