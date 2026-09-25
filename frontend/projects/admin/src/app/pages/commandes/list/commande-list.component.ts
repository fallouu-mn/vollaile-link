import { Component, OnInit } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CommandeService } from '../../../services/commande.service';
import { CommandeDTO } from '../../../dto/commande.dto';
import { ClientService } from '../../../services/client.service';
import { ClientDTO } from '../../../dto/client.dto';
import { ProductService } from '../../../services/product.service';
import { ProductDTO } from '../../../dto/product.dto';
import { ExportService } from '../../../services/export.service';
import { Router } from '@angular/router';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { ViewChild } from '@angular/core';

@Component({
  selector: 'app-commande-list',
  standalone: false,
  templateUrl: './commande-list.component.html',
  styleUrls: ['./commande-list.component.scss']
})
export class CommandeListComponent implements OnInit {
  commandes: CommandeDTO[] = [];
  clients: ClientDTO[] = [];
  produits: ProductDTO[] = [];
  displayedColumns: string[] = ['id', 'clientNom', 'produitNom', 'quantite', 'prixUnitaire', 'statut', 'dateLivraisonPrev', 'actions'];
  dataSource!: MatTableDataSource<CommandeDTO>;
  isLoading = false;
  isExporting = false;

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  // Maps for quick lookup
  clientMap: Map<number, string> = new Map();
  produitMap: Map<number, string> = new Map();

  constructor(
    private commandeService: CommandeService,
    private clientService: ClientService,
    private productService: ProductService,
    private exportService: ExportService,
    private snackBar: MatSnackBar,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading = true;

    // Fetch all data in parallel
    const commandes$ = this.commandeService.getAllCommandes();
    const clients$ = this.clientService.getAllClients();
    const produits$ = this.productService.getAllProducts();

    // In a real implementation, we would use forkJoin or similar
    // For simplicity, we'll chain the observables
    commandes$.subscribe({
      next: (commandes) => {
        this.commandes = commandes;
        this.dataSource = new MatTableDataSource(this.commandes);
        this.dataSource.paginator = this.paginator;
        this.dataSource.sort = this.sort;

        // Load clients and produits
        clients$.subscribe({
          next: (clients) => {
            this.clients = clients;
            // Create client map
            this.clientMap.clear();
            clients.forEach(client => {
              if (client.id && client.nom && client.prenom) {
                this.clientMap.set(client.id, `${client.prenom} ${client.nom}`);
              }
            });

            produits$.subscribe({
              next: (produits) => {
                this.produits = produits;
                // Create produit map
                this.produitMap.clear();
                produits.forEach(produit => {
                  if (produit.id && produit.name) {
                    this.produitMap.set(produit.id, produit.name);
                  }
                });

                this.isLoading = false;
              },
              error: (error) => {
                console.error('Error loading produits:', error);
                this.isLoading = false;
              }
            });
          },
          error: (error) => {
            console.error('Error loading clients:', error);
            this.isLoading = false;
          }
        });
      },
      error: (error) => {
        console.error('Error loading commandes:', error);
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

  /** Export CSV journalisé côté serveur (CDC §2.5). */
  exportCsv(): void {
    this.isExporting = true;
    this.exportService.download('commandes').subscribe({
      next: () => {
        this.isExporting = false;
        this.snackBar.open('Export téléchargé et journalisé', 'Fermer', { duration: 4000 });
      },
      error: (error) => {
        console.error('Error exporting commandes:', error);
        this.isExporting = false;
        this.snackBar.open('Erreur lors de l\'export', 'Fermer', { duration: 5000 });
      }
    });
  }

  deleteCommande(id: number): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer cette commande ?')) {
      this.commandeService.deleteCommande(id).subscribe({
        next: () => {
          this.loadData();
        },
        error: (error) => {
          console.error('Error deleting commande:', error);
        }
      });
    }
  }

  updateStatut(commande: CommandeDTO, newStatut: string): void {
    this.commandeService.updateStatut(commande.id!, newStatut).subscribe({
      next: (updatedCommande) => {
        // Update the commande in the list
        const index = this.commandes.findIndex(c => c.id === commande.id);
        if (index !== -1) {
          this.commandes[index] = updatedCommande;
          this.dataSource.data = this.commandes;
        }
      },
      error: (error) => {
        console.error('Error updating commande status:', error);
      }
    });
  }

  viewDetails(id: number): void {
    this.router.navigate([`../detail/${id}`]);
  }

  // Helper to get client name
  getClientName(clientId: number): string {
    return this.clientMap.get(clientId) || `Client #${clientId}`;
  }

  // Helper to get produit name
  getProduitName(produitId: number): string {
    return this.produitMap.get(produitId) || `Produit #${produitId}`;
  }

  // Helper to get statut variant for badge
  getStatutVariant(statut: string): 'success' | 'warning' | 'info' | 'default' {
    switch (statut) {
      case 'PAYEE':
      case 'EN_PREPARATION':
      case 'EXPEDIEE':
      case 'LIVREE':
        return 'success';
      case 'EN_ATTENTE_PAIEMENT':
        return 'info';
      case 'ANNULEE':
        return 'warning';
      default:
        return 'default';
    }
  }
}