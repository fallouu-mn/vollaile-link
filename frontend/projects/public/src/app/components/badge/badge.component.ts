import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-badge',
  templateUrl: './badge.component.html',
  styleUrls: ['./badge.component.scss'],
  standalone: true,
  imports: [CommonModule]
})
export class BadgeComponent {
  @Input() text: string = '';
  @Input() variant: 'success' | 'warning' | 'info' | 'default' = 'default';
  @Input() pill: boolean = false;

  // Map variant to CSS classes
  get variantClass(): string {
    switch (this.variant) {
      case 'success': return 'badge-success';
      case 'warning': return 'badge-warning';
      case 'info': return 'badge-info';
      default: return 'badge-default';
    }
  }

  // Map variant to background color for subtle animation
  get backgroundColor(): string {
    switch (this.variant) {
      case 'success': return '#28a745';
      case 'warning': return '#ffc107';
      case 'info': return '#17a2b8';
      default: return '#6c757d';
    }
  }
}