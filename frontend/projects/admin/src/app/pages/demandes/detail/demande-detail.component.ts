import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { DemandeService } from '../../../services/demande.service';
import { DemandeDTO } from '../../../dto/demande.dto';
import { ClientService } from '../../../services/client.service';
import { ClientDTO } from '../../../dto/client.dto';
import { ProductService } from '../../../services/product.service';
import { ProductDTO } from '../../../dto/product.dto';
import { MouvementStockDTO } from '../../../dto/mouvement-stock.dto';
import { ActivatedRoute, Router } from '@angular/router';
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
import { MatCardModule } from '@angular/material/card';
import { MatListModule } from '@angular/material/list';
import { MatDividerModule } from '@angular/material/divider';
import { BadgeComponent } from '../../../components/badge/badge.component';

@Component({
  selector: 'app-demande-detail',
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
    MatCardModule,
    MatListModule,
    MatDividerModule,
    BadgeComponent
  ],
  templateUrl: './demande-detail.component.html',
  styleUrls: ['./demande-detail.component.scss']
})
export class DemandeDetailComponent implements OnInit {
  demande: DemandeDTO | null = null;
  client: ClientDTO | null = null;
  produit: ProductDTO | null = null;
  mouvements: MouvementStockDTO[] = [];
  isLoading = false;
  isConverting = false;
  statusForm: FormGroup;
  displayedMouvementColumns: string[] = ['type', 'quantite', 'motif', 'createdAt'];

  constructor(
    private fb: FormBuilder,
    private demandeService: DemandeService,
    private clientService: ClientService,
    private productService: ProductService,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.statusForm = this.fb.group({
      statut: [''],
      commentairesAdmin: ['']
    });
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadDemande(+id);
    }
  }

  loadDemande(id: number): void {
    this.isLoading = true;
    this.demandeService.getDemandeById(id).subscribe({
      next: (demande) => {
        this.demande = demande;
        this.loadClient(demande.clientId);
        if (demande.produitId) {
          this.loadProduit(demande.produitId);
        }
        this.loadMouvementsForDemande(id);
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading demande:', error);
        this.isLoading = false;
      }
    });
  }

  loadClient(clientId: number): void {
    this.clientService.getClientById(clientId).subscribe({
      next: (client) => {
        this.client = client;
      },
      error: (error) => {
        console.error('Error loading client:', error);
      }
    });
  }

  loadProduit(produitId: number): void {
    this.productService.getProductById(produitId).subscribe({
      next: (produit) => {
        this.produit = produit;
      },
      error: (error) => {
        console.error('Error loading produit:', error);
      }
    });
  }

  /**
   * Une demande ne consomme pas de stock : c'est une intention d'achat.
   * Le stock n'est engagé qu'à la conversion en commande, qui crée la
   * réservation (cf. CommandeService.createCommande).
   *
   * L'ancien appel `by-reference` visait un endpoint inexistant et ne
   * retournait de toute façon rien d'exploitable : on affiche donc un
   * état vide explicite plutôt qu'un tableau de mouvements trompeur.
   */
  loadMouvementsForDemande(demandeId: number): void {
    this.mouvements = [];
  }

  updateStatut(newStatut: string, commentairesAdmin: string): void {
    if (!this.demande) return;
    this.demandeService.updateStatut(this.demande.id!, newStatut, commentairesAdmin).subscribe({
      next: (updatedDemande) => {
        this.demande = updatedDemande;
      },
      error: (error) => {
        console.error('Error updating demande status:', error);
      }
    });
  }

  onSubmitStatusChange(): void {
    if (this.statusForm.valid && this.demande) {
      const { statut, commentairesAdmin } = this.statusForm.value;
      this.updateStatut(statut, commentairesAdmin);
    }
  }

  convertToCommande(): void {
    if (!this.demande) return;
    this.isConverting = true;
    this.demandeService.convertToCommande(this.demande.id!).subscribe({
      next: (response) => {
        // After conversion, navigate to the commande detail if created
        this.isConverting = false;
        this.router.navigate(['/commandes']); // Or we could navigate to the specific commande if we had its ID
      },
      error: (error) => {
        console.error('Error converting demande to commande:', error);
        this.isConverting = false;
      }
    });
  }

  // Helper to get statut variant for badge
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

  // Helper to get mouvement type color
  getMouvementTypeColor(type: string): string {
    const colors: Record<string, string> = {
      'ENTREE_STOCK': '#4caf50', // green
      'SORTIE_STOCK': '#f44336', // red
      'RESERVATION': '#ff9800', // orange
      'LIBERATION_RESERVATION': '#9c27b0', // purple
      'VENTE': '#2196f3', // blue
      'AJUSTEMENT': '#607d8b', // blue-gray
      'PERTE': '#795548' // brown
    };
    return colors[type] || '#9e9e9e';
  }

  // Helper to get mouvement type label
  getMouvementTypeLabel(type: string): string {
    const labels: Record<string, string> = {
      'ENTREE_STOCK': 'Entrée de stock',
      'SORTIE_STOCK': 'Sortie de stock',
      'RESERVATION': 'Réservation',
      'LIBERATION_RESERVATION': 'Libération de réservation',
      'VENTE': 'Vente',
      'AJUSTEMENT': 'Ajustement',
      'PERTE': 'Perte'
    };
    return labels[type] || type;
  }
}