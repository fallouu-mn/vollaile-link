import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { AvailabilityService } from './availability.service';
import { WhatsAppButtonComponent } from '../../components/whatsapp-button/whatsapp-button.component';
import { BadgeComponent } from '../../components/badge/badge.component';

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

export interface OfferFilters {
  productName?: string;
  minQuantity?: number;
  maxQuantity?: number;
  minPrice?: number;
  maxPrice?: number;
}

@Component({
  selector: 'app-availability',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, WhatsAppButtonComponent, BadgeComponent],
  templateUrl: './availability.component.html',
  styleUrls: ['./availability.component.scss']
})
export class AvailabilityComponent implements OnInit {
  offers: OfferPublicDTO[] = [];
  filteredOffers: OfferPublicDTO[] = [];
  isLoading = false;
  errorMessage: string | null = null;

  filterForm: FormGroup;

  constructor(
    private availabilityService: AvailabilityService,
    private fb: FormBuilder
  ) {
    this.filterForm = this.fb.group({
      productName: [''],
      minQuantity: [null],
      maxQuantity: [null],
      minPrice: [null],
      maxPrice: [null]
    });
  }

  ngOnInit(): void {
    this.loadOffers();
  }

  loadOffers(): void {
    this.isLoading = true;
    this.errorMessage = null;

    const filters = this.filterForm.value;
    // Convert empty strings to null for numeric fields
    Object.keys(filters).forEach(key => {
      if (filters[key] === '') {
        filters[key] = null;
      }
    });

    this.availabilityService.getOffers(filters).subscribe({
      next: (data) => {
        this.offers = data;
        this.filteredOffers = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading offers:', err);
        this.errorMessage = 'Impossible de charger les offres. Veuillez réessayer plus tard.';
        this.isLoading = false;
      }
    });
  }

  onFilter(): void {
    this.loadOffers();
  }

  onReset(): void {
    this.filterForm.reset();
    this.loadOffers();
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