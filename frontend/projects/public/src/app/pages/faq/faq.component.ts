import { Component, OnInit } from '@angular/core';
import { FAQService } from '../../services/faq.service';
import { CommonModule } from '@angular/common';

export interface FAQDTO {
  id: number;
  question: string;
  answer: string;
  displayOrder: number;
}

@Component({
  selector: 'app-faq',
  templateUrl: './faq.component.html',
  styleUrls: ['./faq.component.scss'],
  standalone: true,
  imports: [CommonModule]
})
export class FAQComponent implements OnInit {
  faqs: FAQDTO[] = [];
  isLoading = false;
  errorMessage: string | null = null;
  // Map to track which FAQs are open (by id)
  openFaqs: Set<number> = new Set();

  constructor(private faqService: FAQService) {}

  ngOnInit(): void {
    this.loadFAQs();
  }

  loadFAQs(): void {
    this.isLoading = true;
    this.errorMessage = null;

    this.faqService.getAllFAQs().subscribe({
      next: (data) => {
        this.faqs = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading FAQs:', err);
        this.errorMessage = 'Impossible de charger les FAQ';
        this.isLoading = false;
      }
    });
  }

  toggleFaq(faqId: number): void {
    if (this.openFaqs.has(faqId)) {
      this.openFaqs.delete(faqId);
    } else {
      this.openFaqs.add(faqId);
    }
  }

  isFaqOpen(faqId: number): boolean {
    return this.openFaqs.has(faqId);
  }
}