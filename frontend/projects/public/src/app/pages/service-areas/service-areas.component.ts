import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PageService } from '../../services/page.service';
import { Meta, Title } from '@angular/platform-browser';

export interface PageDTO {
  id: number;
  title: string;
  slug: string;
  content: string;
  metaTitle?: string;
  metaDescription?: string;
  ogImage?: string;
}

@Component({
  selector: 'app-service-areas',
  templateUrl: './service-areas.component.html',
  styleUrls: ['./service-areas.component.scss'],
  standalone: true,
  imports: [CommonModule]
})
export class ServiceAreasComponent implements OnInit {
  pageData: PageDTO | null = null;
  isLoading = false;
  errorMessage: string | null = null;

  constructor(
    private pageService: PageService,
    private meta: Meta,
    private title: Title
  ) {}

  ngOnInit(): void {
    this.loadPage();
  }

  loadPage(): void {
    this.isLoading = true;
    this.errorMessage = null;

    this.pageService.getPageBySlug('service-areas').subscribe({
      next: (data) => {
        this.pageData = data;
        this.isLoading = false;
        // Set meta tags for SEO
        this.title.setTitle(data.metaTitle || data.title || 'Zones desservies - Vollaile Link');
        this.meta.updateTag({ name: 'description', content: data.metaDescription || '' });
        this.meta.updateTag({ property: 'og:title', content: data.metaTitle || data.title || '' });
        this.meta.updateTag({ property: 'og:description', content: data.metaDescription || '' });
        if (data.ogImage) {
          this.meta.updateTag({ property: 'og:image', content: data.ogImage });
        }
        this.meta.updateTag({ property: 'og:type', content: 'website' });
      },
      error: (err) => {
        console.error('Error loading service-areas page:', err);
        this.errorMessage = 'Impossible de charger la page';
        this.isLoading = false;
      }
    });
  }
}