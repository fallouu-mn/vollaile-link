import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

export interface PriceEditDialogData {
  offerId: number;
  productNom?: string;
  prixActuel: number;
  stock: {
    quantityTotal: number;
    quantityReserved: number;
    quantitySold: number;
    quantityAvailable: number;
  };
}

/**
 * Saisie d'une variation de prix.
 *
 * Le motif est obligatoire : sans justification, la variation n'est pas
 * traçable et le serveur la refuse (CDC §2.5).
 */
@Component({
  selector: 'app-price-edit-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule
  ],
  template: `
    <h2 mat-dialog-title>Modifier le prix — Offre #{{ data.offerId }}</h2>

    <mat-dialog-content>
      <p class="context">
        {{ data.productNom || 'Produit' }} — prix actuel
        <strong>{{ data.prixActuel | number:'1.0-0' }} FCFA</strong>
      </p>

      <div class="stock-info">
        <span>Stock : {{ data.stock.quantityTotal }} total,
          {{ data.stock.quantityReserved }} réservé(s),
          {{ data.stock.quantitySold }} vendu(s)</span>
        <span>Disponible : <strong>{{ data.stock.quantityAvailable }}</strong></span>
      </div>

      <form [formGroup]="form" class="price-form">
        <mat-form-field appearance="fill" class="full-width">
          <mat-label>Nouveau prix unitaire (FCFA) *</mat-label>
          <input matInput type="number" formControlName="prix" min="0" step="50">
          <mat-error *ngIf="form.get('prix')?.hasError('required')">Le prix est obligatoire</mat-error>
          <mat-error *ngIf="form.get('prix')?.hasError('min')">Le prix ne peut pas être négatif</mat-error>
          <mat-hint *ngIf="variation !== 0">
            Variation : {{ variation > 0 ? '+' : '' }}{{ variation | number:'1.0-0' }} FCFA
            ({{ variationPercent | number:'1.1-1' }} %)
          </mat-hint>
        </mat-form-field>

        <mat-form-field appearance="fill" class="full-width">
          <mat-label>Motif de la variation *</mat-label>
          <textarea matInput formControlName="motif" rows="2"
                    placeholder="Ex: révision tarifaire trimestrielle"></textarea>
          <mat-error *ngIf="form.get('motif')?.hasError('required')">
            Un motif est obligatoire pour la traçabilité
          </mat-error>
        </mat-form-field>
      </form>

      <p class="warning">
        <mat-icon>info</mat-icon>
        Ce changement est historisé et inscrit au journal d'audit.
      </p>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button (click)="onCancel()">Annuler</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid" (click)="onConfirm()">
        Enregistrer
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .context { color: var(--text-secondary); font-size: 13px; margin: 0 0 8px; }
    .stock-info {
      display: flex; justify-content: space-between; flex-wrap: wrap;
      gap: 8px; font-size: 12px; color: var(--text-muted);
      padding: 10px 12px; background: var(--surface-alt, #f8f9fa);
      border-radius: 6px; margin-bottom: 16px;
    }
    .full-width { width: 100%; }
    .warning {
      display: flex; align-items: center; gap: 6px;
      font-size: 12px; color: var(--text-muted); margin: 0;
    }
    .warning .material-icons { font-size: 16px; }
  `]
})
export class PriceEditDialogComponent {
  form: FormGroup;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: PriceEditDialogData,
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<PriceEditDialogComponent>
  ) {
    this.form = this.fb.group({
      prix: [data.prixActuel, [Validators.required, Validators.min(0)]],
      motif: ['', [Validators.required, Validators.maxLength(500)]]
    });
  }

  get variation(): number {
    return Number(this.form.get('prix')?.value || 0) - this.data.prixActuel;
  }

  get variationPercent(): number {
    if (!this.data.prixActuel) {
      return 0;
    }
    return (this.variation / this.data.prixActuel) * 100;
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onConfirm(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.dialogRef.close({
      prix: Number(this.form.get('prix')?.value),
      motif: this.form.get('motif')?.value
    });
  }
}
