import { Component, OnInit } from '@angular/core';
import { CommandeService } from '../../../services/commande.service';
import { CommandeDTO } from '../../../dto/commande.dto';
import { ClientService } from '../../../services/client.service';
import { ClientDTO } from '../../../dto/client.dto';
import { ProductService } from '../../../services/product.service';
import { ProductDTO } from '../../../dto/product.dto';
import { ActivatedRoute, Router } from '@angular/router';
import { MouvementStockService } from '../../../services/mouvement-stock.service';
import { MouvementStockDTO } from '../../../dto/mouvement-stock.dto';

@Component({
  selector: 'app-commande-detail',
  standalone: false,
  templateUrl: './commande-detail.component.html',
  styleUrls: ['./commande-detail.component.scss']
})
export class CommandeDetailComponent implements OnInit {
  commande: CommandeDTO | null = null;
  client: ClientDTO | null = null;
  produit: ProductDTO | null = null;
  mouvements: MouvementStockDTO[] = [];
  isLoading = false;

  constructor(
    private commandeService: CommandeService,
    private clientService: ClientService,
    private productService: ProductService,
    private route: ActivatedRoute,
    private router: Router,
    private mouvementStockService: MouvementStockService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadCommande(+id);
    }
  }

  loadCommande(id: number): void {
    this.isLoading = true;
    this.commandeService.getCommandeById(id).subscribe({
      next: (commande) => {
        this.commande = commande;
        this.loadClient(commande.clientId);
        this.loadProduit(commande.produitId);
        this.loadMouvementsForCommande(commande);
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading commande:', error);
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
   * Charge les mouvements de l'offre liée à la commande.
   *
   * L'ancien endpoint `by-reference` n'existe pas côté backend : les
   * mouvements sont rattachés à l'offre, et le stock étant porté par
   * l'offre, c'est le bon périmètre. On ne retient ensuite que les
   * mouvements de cette commande (préfixe de référence CMD-).
   */
  loadMouvementsForCommande(commande: CommandeDTO): void {
    if (!commande.offreId) {
      this.mouvements = [];
      return;
    }

    this.mouvementStockService.getMouvementsByOffer(commande.offreId).subscribe({
      next: (mouvements) => {
        const prefix = `CMD-`;
        this.mouvements = mouvements.filter(
          m => m.reference && (m.reference.startsWith(`${prefix}`) || m.reference.includes(`-${commande.id}`))
        );
      },
      error: (error) => {
        console.error('Error loading mouvements for commande:', error);
      }
    });
  }

  updateStatut(newStatut: string, commentaires: string): void {
    if (!this.commande) return;
    this.commandeService.updateStatut(this.commande.id!, newStatut, commentaires).subscribe({
      next: (updatedCommande) => {
        this.commande = updatedCommande;
      },
      error: (error) => {
        console.error('Error updating commande status:', error);
      }
    });
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