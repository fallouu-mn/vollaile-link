import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { AuditService, AuditLogEntry, AUDIT_EVENT_TYPES } from '../../services/audit.service';
import { ExportService } from '../../services/export.service';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

/**
 * Journal d'audit (CDC §2.5).
 *
 * Chaque opération métier significant est tracée : mouvements de stock,
 * variations de prix, changements de statut, connexions, exports.
 */
@Component({
  selector: 'app-audit-log',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatTableModule,
    MatToolbarModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatTooltipModule,
    MatProgressBarModule,
    MatPaginatorModule,
    MatSnackBarModule
  ],
  templateUrl: './audit-log.component.html',
  styleUrls: ['./audit-log.component.scss']
})
export class AuditLogComponent implements OnInit {
  entries: AuditLogEntry[] = [];
  eventTypes = AUDIT_EVENT_TYPES;
  displayedColumns: string[] = ['id', 'eventType', 'description', 'actor', 'ipAddress', 'eventTimestamp', 'metadata'];

  selectedType = '';
  filterText = '';
  isLoading = false;
  isExporting = false;
  errorMessage: string | null = null;
  expandedId: number | null = null;

  constructor(
    private auditService: AuditService,
    private exportService: ExportService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadLog();
  }

  loadLog(): void {
    this.isLoading = true;
    this.errorMessage = null;

    this.auditService.getAuditLog(this.selectedType || undefined, 0, 200).subscribe({
      next: (entries) => {
        this.entries = entries;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading audit log:', error);
        this.errorMessage = 'Impossible de charger le journal d\'audit.';
        this.isLoading = false;
      }
    });
  }

  onTypeChange(): void {
    this.loadLog();
  }

  /**
   * Export du journal, avec le filtre actuellement sélectionné.
   * Exporter la piste d'audit est lui-même tracé : l'administrateur en
   * est informé dans la notification.
   */
  exportCsv(): void {
    this.isExporting = true;
    const params: { [key: string]: string } = this.selectedType
      ? { eventType: this.selectedType }
      : {};

    this.exportService.download('audit', params).subscribe({
      next: () => {
        this.isExporting = false;
        this.snackBar.open(
          'Export téléchargé. Cette exportation est elle-même journalisée.',
          'Fermer',
          { duration: 6000 }
        );
      },
      error: (error) => {
        console.error('Error exporting audit log:', error);
        this.isExporting = false;
        this.snackBar.open('Erreur lors de l\'export', 'Fermer', { duration: 5000 });
      }
    });
  }

  /** Filtre applicatif sur le texte saisi, sans re-requêter. */
  get filteredEntries(): AuditLogEntry[] {
    const q = this.filterText.trim().toLowerCase();
    if (!q) {
      return this.entries;
    }
    return this.entries.filter(e =>
      (e.eventDescription ?? '').toLowerCase().includes(q) ||
      (e.administratorPhone ?? '').toLowerCase().includes(q) ||
      e.eventType.toLowerCase().includes(q)
    );
  }

  toggleMetadata(entry: AuditLogEntry): void {
    this.expandedId = this.expandedId === entry.id ? null : entry.id;
  }

  getEventLabel(type: string): string {
    return this.eventTypes.find(t => t.value === type)?.label ?? type;
  }

  getEventClass(type: string): string {
    if (type.startsWith('STOCK_LIBERATION') || type.startsWith('STOCK_SORTIE')) return 'ev-red';
    if (type.startsWith('STOCK_ENTREE') || type.startsWith('STOCK_VENTE') || type.includes('SUCCESS')) return 'ev-green';
    if (type.startsWith('STOCK_RESERVATION') || type.startsWith('STOCK_AJUSTEMENT')) return 'ev-amber';
    if (type.startsWith('PRIX') || type.startsWith('STOCK_LIBERATION')) return 'ev-purple';
    if (type.includes('FAILURE')) return 'ev-red';
    if (type.includes('STATUT')) return 'ev-blue';
    return 'ev-gray';
  }

  formatDate(iso: string): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleString('fr-FR');
  }

  /** Le JSONB est renvoyé brut : on le formate pour la lecture. */
  prettyMetadata(metadata: string | undefined): string {
    if (!metadata) return '';
    try {
      return JSON.stringify(JSON.parse(metadata), null, 2);
    } catch {
      return metadata;
    }
  }
}
