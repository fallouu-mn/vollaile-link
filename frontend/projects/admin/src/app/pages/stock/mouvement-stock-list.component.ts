import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';
import { MouvementStockService } from '../../services/mouvement-stock.service';
import { ExportService } from '../../services/export.service';
import { MouvementStockDTO } from '../../dto/mouvement-stock.dto';
import { ProductService } from '../../services/product.service';
import { ProductDTO } from '../../dto/product.dto';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatTooltipModule } from '@angular/material/tooltip';

@Component({
  selector: 'app-mouvement-stock-list',
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
  templateUrl: './mouvement-stock-list.component.html',
  styleUrls: ['./mouvement-stock-list.component.scss']
})
export class MouvementStockListComponent implements OnInit {
  mouvements: MouvementStockDTO[] = [];
  produits: ProductDTO[] = [];
  isLoading = false;
  isExporting = false;
  errorMessage: string | null = null;

  displayedColumns: string[] = [
    'id', 'typeMouvement', 'offre', 'quantite', 'quantiteApresMouvement', 'referenceInfo', 'motif', 'date'
  ];
  dataSource!: MatTableDataSource<MouvementStockDTO>;

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  private produitMap: Map<number, string> = new Map();

  constructor(
    private mouvementStockService: MouvementStockService,
    private productService: ProductService,
    private exportService: ExportService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading = true;
    this.errorMessage = null;

    // forkJoin en parallèle : les anciens subscribe imbriqués
    // déclenchaient les requêtes en série et masquaient les erreurs.
    forkJoin({
      mouvements: this.mouvementStockService.getAllMouvements(),
      produits: this.productService.getAllProducts()
    }).subscribe({
      next: ({ mouvements, produits }) => {
        this.mouvements = mouvements;
        this.produits = produits;
        this.produitMap.clear();
        produits.forEach(produit => {
          if (produit.id && produit.name) {
            this.produitMap.set(produit.id, produit.name);
          }
        });

        this.dataSource = new MatTableDataSource(this.mouvements);
        this.dataSource.paginator = this.paginator;
        this.dataSource.sort = this.sort;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading mouvements:', error);
        this.errorMessage = 'Impossible de charger les mouvements de stock.';
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

  /**
   * Export de l'état du stock par offre.
   * Colonnes total / réservé / vendu / disponible : permet de vérifier
   * la règle du CDC hors de l'application.
   */
  exportCsv(): void {
    this.isExporting = true;
    this.exportService.download('stock').subscribe({
      next: () => {
        this.isExporting = false;
        this.snackBar.open('Export téléchargé et journalisé', 'Fermer', { duration: 4000 });
      },
      error: (error) => {
        console.error('Error exporting stock:', error);
        this.isExporting = false;
        this.snackBar.open('Erreur lors de l\'export', 'Fermer', { duration: 5000 });
      }
    });
  }

  /**
   * L'offre est l'unité de stock. L'afficher évite de croire que
   * le stock d'un produit est mutualisé entre producteurs.
   */
  getOffreLabel(mouvement: MouvementStockDTO): string {
    if (!mouvement.offerId) {
      return 'Offre historique';
    }
    const produit = this.getProduitName(mouvement.produitId);
    return `Offre #${mouvement.offerId} — ${produit}`;
  }

  getProduitName(produitId: number): string {
    return this.produitMap.get(produitId) || `Produit #${produitId}`;
  }

  getReferenceInfo(mouvement: MouvementStockDTO): string {
    if (!mouvement.reference) {
      return '-';
    }
    return mouvement.reference;
  }

  getTypeMouvementLabel(type: string): string {
    const labels: Record<string, string> = {
      'ENTREE': 'Entrée',
      'SORTIE': 'Sortie',
      'RESERVATION': 'Réservation',
      'LIBERATION_RESERVATION': 'Libération de réservation',
      'VENTE': 'Vente',
      'AJUSTEMENT': 'Ajustement',
      'INVENTAIRE': 'Inventaire'
    };
    return labels[type] || type;
  }

  getTypeMouvementColor(type: string): string {
    const colors: Record<string, string> = {
      'ENTREE': '#4caf50',
      'SORTIE': '#f44336',
      'RESERVATION': '#ff9800',
      'LIBERATION_RESERVATION': '#607d8b',
      'VENTE': '#2196f3',
      'AJUSTEMENT': '#9c27b0',
      'INVENTAIRE': '#607d8b'
    };
    return colors[type] || '#9e9e9e';
  }
}
