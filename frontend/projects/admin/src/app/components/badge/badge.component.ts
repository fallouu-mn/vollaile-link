import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-badge',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './badge.component.html',
  styleUrls: ['./badge.component.scss']
})
export class BadgeComponent {
  @Input() text: string = '';
  @Input() variant: 'success' | 'warning' | 'info' | 'default' = 'default';
  @Input() pill: boolean = false;

  get variantClass(): string {
    return `badge-${this.variant}`;
  }
}