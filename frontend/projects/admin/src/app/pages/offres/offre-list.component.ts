import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { OfferService, AdminOfferDTO, OfferStatus } from '../../services/offer.service';
import { PriceEditDialogComponent } from './price-edit-dialog/price-edit-dialog.component';

/**
 * Gestion des offres : prix, statut et état du stock.
 *
 * Le stock est porté par l'offre (disponible = total − réservé − vendu).
 * Toute modification de prix ou de statut est historisée et auditée côté
 * serveur : l'interface ne fait qu'appeler l'API et afficher le retour.
 */
@Component({
  selector: 'app-offre-list',
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
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatDialogModule,
    MatSnackBarModule,
    MatProgressBarModule,
    MatTooltipModule
  ],
  templateUrl: './offre-list.component.html',
  styleUrls: ['./offre-list.component.scss']
})
export class OffreListComponent implements OnInit {
  offres: AdminOfferDTO[] = [];
  displayedColumns: string[] = [
    'id', 'offre', 'prix', 'stock', 'disponible', 'statut', 'actions'
  ];
  dataSource!: MatTableDataSource<AdminOfferDTO>;

  isLoading = false;
  errorMessage: string | null = null;

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  constructor(
    private offerService: OfferService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadOffres();
  }

  loadOffres(): void {
    this.isLoading = true;
    this.errorMessage = null;

    this.offerService.getOffers().subscribe({
      next: (offres) => {
        this.offres = offres;
        this.dataSource = new MatTableDataSource(this.offres);
        this.dataSource.paginator = this.paginator;
        this.dataSource.sort = this.sort;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading offres:', error);
        this.errorMessage = 'Impossible de charger les offres.';
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

  formatPrice(prix: number | undefined): string {
    if (prix === undefined || prix === null) {
      return '—';
    }
    return `${new Intl.NumberFormat('fr-FR').format(prix)} FCFA`;
  }

  getStatutLabel(statut: OfferStatus | undefined): string {
    const labels: Record<string, string> = {
      'DRAFT': 'Brouillon',
      'ACTIVE': 'Active',
      'EXPIRED': 'Expirée',
      'WITHDRAWN': 'Retirée'
    };
    return statut ? labels[statut] ?? statut : '—';
  }

  /** La couleur reflète la disponibilité réelle, pas seulement le statut. */
  getStatutClass(offre: AdminOfferDTO): string {
    if (offre.quantityAvailable === 0) return 'statut-epuise';
    switch (offre.status) {
      case 'ACTIVE': return 'statut-actif';
      case 'DRAFT': return 'statut-brouillon';
      case 'EXPIRED': return 'statut-expire';
      case 'WITHDRAWN': return 'statut-retire';
      default: return '';
    }
  }

  editPrice(offre: AdminOfferDTO): void {
    const ref = this.dialog.open(PriceEditDialogComponent, {
      width: '460px',
      data: {
        offerId: offre.offerId,
        productNom: offre.productNom,
        prixActuel: offre.unitPrice ?? 0,
        stock: offre
      }
    });

    ref.afterClosed().subscribe((result?: { prix: number; motif: string }) => {
      if (!result) {
        return;
      }
      this.offerService.updatePrice(offre.offerId, result.prix, result.motif).subscribe({
        next: () => {
          this.snackBar.open('Prix mis à jour et historisé', 'Fermer', { duration: 4000 });
          this.loadOffres();
        },
        error: (error) => {
          this.snackBar.open(
            error?.error?.error ?? 'Erreur lors de la mise à jour du prix',
            'Fermer',
            { duration: 6000 }
          );
        }
      });
    });
  }

  changeStatus(offre: AdminOfferDTO, statut: OfferStatus): void {
    const motif = window.prompt(
      `Statut de l'offre #${offre.offerId} : ${this.getStatutLabel(offre.status)} → ${this.getStatutLabel(statut)}\n\nMotif (facultatif) :`
    );
    if (motif === null) {
      return;
    }

    this.offerService.updateStatus(offre.offerId, statut, motif).subscribe({
      next: () => {
        this.snackBar.open('Statut mis à jour', 'Fermer', { duration: 4000 });
        this.loadOffres();
      },
      error: (error) => {
        this.snackBar.open(
          error?.error?.error ?? 'Erreur lors du changement de statut',
          'Fermer',
          { duration: 7000 }
        );
      }
    });
  }
}
