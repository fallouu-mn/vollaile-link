import { Component, OnInit, ViewChild } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { DemandeService } from '../../../services/demande.service';
import { DemandeDTO } from '../../../dto/demande.dto';
import { Router } from '@angular/router';
import { ClientService } from '../../../services/client.service';
import { ClientDTO } from '../../../dto/client.dto';
import { ProductService } from '../../../services/product.service';
import { ProductDTO } from '../../../dto/product.dto';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatSortModule } from '@angular/material/sort';
import { BadgeComponent } from '../../../components/badge/badge.component';

@Component({
  selector: 'app-demande-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    ReactiveFormsModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    BadgeComponent
  ],
  templateUrl: './demande-list.component.html',
  styleUrls: ['./demande-list.component.scss']
})
export class DemandeListComponent implements OnInit {
  demandes: DemandeDTO[] = [];
  clients: ClientDTO[] = [];
  produits: ProductDTO[] = [];
  clientMap = new Map<number, string>();
  produitMap = new Map<number, string>();

  displayedColumns: string[] = ['id', 'clientNom', 'clientPrenom', 'produitNom', 'quantiteSouhaitee', 'statut', 'dateSouhaitee', 'actions'];
  dataSource!: MatTableDataSource<DemandeDTO>;
  isLoading = false;

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  constructor(
    private demandeService: DemandeService,
    private clientService: ClientService,
    private productService: ProductService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading = true;

    // Fetch all data in parallel
    const demandes$ = this.demandeService.getAllDemandes();
    const clients$ = this.clientService.getAllClients();
    const produits$ = this.productService.getAllProducts();

    demandes$.subscribe({
      next: (demandes) => {
        this.demandes = demandes;
        this.dataSource = new MatTableDataSource(this.demandes);
        this.dataSource.paginator = this.paginator;
        this.dataSource.sort = this.sort;

        // Load clients and produits for name mapping
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
        console.error('Error loading demandes:', error);
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

  deleteDemande(id: number): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer cette demande ?')) {
      this.demandeService.deleteDemande(id).subscribe({
        next: () => {
          this.loadData();
        },
        error: (error) => {
          console.error('Error deleting demande:', error);
        }
      });
    }
  }

  updateStatut(demande: DemandeDTO, newStatut: string): void {
    this.demandeService.updateStatut(demande.id!, newStatut).subscribe({
      next: (updatedDemande) => {
        // Update the demande in the list
        const index = this.demandes.findIndex(d => d.id === demande.id);
        if (index !== -1) {
          this.demandes[index] = updatedDemande;
          this.dataSource.data = this.demandes;
        }
      },
      error: (error) => {
        console.error('Error updating demande status:', error);
      }
    });
  }

  viewDetails(id: number): void {
    this.router.navigate([`../detail/${id}`]);
  }

  getStatutVariant(statut: string): 'success' | 'warning' | 'info' | 'default' {
    switch (statut) {
      case 'VALIDEE':
        return 'success';
      case 'EN_COURS':
        return 'info';
      case 'REFUSEE':
      case 'EXPIREE':
      case 'SANS_SUITE':
        return 'warning';
      default:
        return 'default';
    }
  }

  // Helper method to get client name from ID
  getClientName(clientId: number): string {
    return this.clientMap.get(clientId) || `Client #${clientId}`;
  }

  // Helper method to get produit name from ID
  getProduitName(produitId: number): string {
    return this.produitMap.get(produitId) || `Produit #${produitId}`;
  }
}