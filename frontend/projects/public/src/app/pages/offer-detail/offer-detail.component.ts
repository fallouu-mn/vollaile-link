import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { OfferDetailService } from './offer-detail.service';
import { Meta, Title } from '@angular/platform-browser';
import { WhatsAppButtonComponent } from '../../components/whatsapp-button/whatsapp-button.component';
import { BadgeComponent } from '../../components/badge/badge.component';
import { CommonModule } from '@angular/common';

export interface OfferPublicDTO {
  id: number;
  productName: string;
  productDescription?: string;
  unitType?: string;
  unitPrice: number;
  quantityAvailable: number;
  totalValue?: number;
  status?: string;
  startsAt?: string;
  expiresAt?: string;
  description?: string;
}

@Component({
  selector: 'app-offer-detail',
  standalone: true,
  imports: [WhatsAppButtonComponent, BadgeComponent, CommonModule],
  templateUrl: './offer-detail.component.html',
  styleUrls: ['./offer-detail.component.scss']
})
export class OfferDetailComponent implements OnInit {
  offer: OfferPublicDTO | null = null;
  isLoading = false;
  errorMessage: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private offerService: OfferDetailService,
    private meta: Meta,
    private title: Title
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const id = Number(params.get('id'));
      if (!isNaN(id)) {
        this.loadOffer(id);
      } else {
        this.errorMessage = "ID d'offre invalide";
      }
    });
  }

  loadOffer(id: number): void {
    this.isLoading = true;
    this.errorMessage = null;

    this.offerService.getOfferById(id).subscribe({
      next: (data) => {
        this.offer = data;
        this.isLoading = false;
        // Set meta tags for SEO
        this.title.setTitle(`${data.productName} - Vollaile Link`);
        this.meta.updateTag({ name: 'description', content: data.productDescription || '' });
        this.meta.updateTag({ property: 'og:title', content: `${data.productName} - Vollaile Link` });
        this.meta.updateTag({ property: 'og:description', content: data.productDescription || '' });
        // Note: we don't have an image for the offer, but we could use a default or leave it out
      },
      error: (err) => {
        console.error('Error loading offer:', err);
        this.errorMessage = "Offre non trouvée ou indisponible";
        this.isLoading = false;
      }
    });
  }

  // Helper method to format currency (FCFA)
  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'XOF'
    }).format(amount);
  }

  // Helper method to format date
  formatDate(dateString: string): string {
    const options: Intl.DateTimeFormatOptions = {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    };
    return new Date(dateString).toLocaleDateString('fr-FR', options);
  }
}