import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { Meta, Title } from '@angular/platform-browser';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { PageService } from '../../services/page.service';
import {
  AvailabilityService,
  OfferPublicDTO
} from '../availability/availability.service';
import { LotMeterComponent } from '../../components/lot-meter/lot-meter.component';
import { RevealDirective } from '../../core/directives/reveal.directive';

/**
 * Landing page.
 *
 * Les lots affichés proviennent de l'API publique : si la base est vide,
 * la section le dit explicitement plutôt que de montrer un exemple
 * présenté comme du live. Le meter du hero, lui, est un exemple
 * pédagogique et le précise.
 */
@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink, LotMeterComponent, RevealDirective],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent implements OnInit {

  pageData: unknown = null;
  isLoading = false;
  errorMessage: string | null = null;

  /** Derniers lots publiés, pour la section « ce qui part en ce moment ». */
  offers: OfferPublicDTO[] = [];
  isLoadingOffers = false;
  offersError = false;

  /** Nombre de lots montrés : la section reste lisible sur grand écran. */
  private static readonly MAX_OFFRES = 3;

  constructor(
    private pageService: PageService,
    private availabilityService: AvailabilityService,
    private meta: Meta,
    private title: Title,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadPageData();
    this.loadOffers();
  }

  private loadPageData(): void {
    this.isLoading = true;
    this.errorMessage = null;

    this.pageService.getPageBySlug('home').subscribe({
      next: (data) => {
        this.pageData = data;
        this.isLoading = false;
        this.applySeo(data.metaTitle, data.metaDescription, data.ogImage, data.title);
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Error loading home page:', err);
        this.errorMessage = 'Impossible de charger la page';
        this.isLoading = false;
        this.applyDefaultSeo();
        this.cdr.markForCheck();
      }
    });
  }

  private loadOffers(): void {
    this.isLoadingOffers = true;
    this.offersError = false;

    this.availabilityService.getOffers().subscribe({
      next: (offers) => {
        // Un lot sans stock disponible n'a rien à montrer.
        this.offers = (offers ?? [])
          .filter(o => o.quantityAvailable > 0)
          .slice(0, HomeComponent.MAX_OFFRES);
        this.isLoadingOffers = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Error loading offers:', err);
        this.offersError = true;
        this.isLoadingOffers = false;
        this.cdr.markForCheck();
      }
    });
  }

  private applySeo(
    metaTitle: string | undefined,
    metaDescription: string | undefined,
    ogImage: string | undefined,
    fallbackTitle: string | undefined
  ): void {
    const title = metaTitle || fallbackTitle || 'Vollaile Link';
    const description = metaDescription
      || 'Vollaile Link centralise les disponibilités de volailles de producteurs '
         + 'locaux au Sénégal. Restaurants, hôtels, traiteurs — trouvez votre '
         + 'approvisionnement en quelques clics.';

    this.title.setTitle(title);
    this.meta.updateTag({ name: 'description', content: description });
    this.meta.updateTag({ property: 'og:title', content: title });
    this.meta.updateTag({ property: 'og:description', content: description });
    this.meta.updateTag({ property: 'og:type', content: 'website' });
    if (ogImage) {
      this.meta.updateTag({ property: 'og:image', content: ogImage });
    }
  }

  private applyDefaultSeo(): void {
    this.title.setTitle("Vollaile Link — Approvisionnement en volailles au Sénégal");
    this.meta.updateTag({
      name: 'description',
      content: 'Vollaile Link centralise les disponibilités de volailles de '
             + 'producteurs locaux au Sénégal.'
    });
  }
}
