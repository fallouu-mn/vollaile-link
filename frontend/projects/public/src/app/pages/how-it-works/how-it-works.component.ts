import { Component, OnInit } from '@angular/core';
import { PageService } from '../../services/page.service';
import { Meta, Title } from '@angular/platform-browser';
import { CommonModule } from '@angular/common';

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
  selector: 'app-how-it-works',
  templateUrl: './how-it-works.component.html',
  styleUrls: ['./how-it-works.component.scss'],
  standalone: true,
  imports: [CommonModule]
})
export class HowItWorksComponent implements OnInit {
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

    this.pageService.getPageBySlug('how-it-works').subscribe({
      next: (data) => {
        this.pageData = data;
        this.isLoading = false;
        // Set meta tags for SEO
        this.title.setTitle(data.metaTitle || data.title || 'Comment ça marche - Vollaile Link');
        this.meta.updateTag({ name: 'description', content: data.metaDescription || '' });
        this.meta.updateTag({ property: 'og:title', content: data.metaTitle || data.title || '' });
        this.meta.updateTag({ property: 'og:description', content: data.metaDescription || '' });
        if (data.ogImage) {
          this.meta.updateTag({ property: 'og:image', content: data.ogImage });
        }
        this.meta.updateTag({ property: 'og:type', content: 'website' });
      },
      error: (err) => {
        console.error('Error loading how-it-works page:', err);
        this.errorMessage = 'Impossible de charger la page';
        this.isLoading = false;
      }
    });
  }
}