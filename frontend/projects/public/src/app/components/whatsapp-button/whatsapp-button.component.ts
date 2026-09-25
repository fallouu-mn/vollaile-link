import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { VOLLAILE_LINK_PUBLIC_CONFIG } from '../../core/config/volaille-link.config';

export interface OfferPublicDTO {
  id: number;
  productName: string;
  commercialName?: string;
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
  selector: 'app-whatsapp-button',
  templateUrl: './whatsapp-button.component.html',
  styleUrls: ['./whatsapp-button.component.scss'],
  standalone: true,
  imports: [CommonModule]
})
export class WhatsAppButtonComponent implements OnInit {
  @Input() offer: OfferPublicDTO | null = null;
  @Input() whatsappNumber: string = VOLLAILE_LINK_PUBLIC_CONFIG.whatsappNumber;
  @Input() buttonLabel: string = 'Envoyer ma demande';
  @Input() showIcon: boolean = true;
  @Input() requestedQuantity?: number;
  @Input() deliveryZone?: string;

  constructor() { }

  ngOnInit(): void {
  }

  get hasWhatsAppNumber(): boolean {
    return this.normalizedWhatsAppNumber.length > 0;
  }

  get requestUrl(): string {
    return this.offer?.id
      ? `/demande-devis?offreId=${this.offer.id}`
      : '/demande-devis';
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

  // WhatsApp integration
  openWhatsApp(): void {
    if (!this.offer || !this.hasWhatsAppNumber) return;

    const quantity = this.requestedQuantity
      ? `${this.requestedQuantity}`
      : '[quantité souhaitée]';
    const product = this.offer.productName;
    const offerName = this.offer.commercialName ?? this.offer.productName;
    const zone = this.deliveryZone || '[zone de livraison]';

    const message = `Bonjour, je souhaite commander ${quantity} ${product}. ` +
                   `J’ai vu votre offre ${offerName} sur votre site. ` +
                   `Ma zone de livraison est ${zone}. ` +
                   `Je souhaite passer par Vollaile Link pour confirmer la disponibilité et les conditions.`;

    const url = `https://wa.me/${this.normalizedWhatsAppNumber}?text=${encodeURIComponent(message)}`;
    window.open(url, '_blank', 'noopener,noreferrer');
  }

  private get normalizedWhatsAppNumber(): string {
    return this.whatsappNumber.replace(/[^0-9]/g, '');
  }
}