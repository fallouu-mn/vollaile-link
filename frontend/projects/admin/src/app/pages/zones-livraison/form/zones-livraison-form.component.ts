import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ZoneLivraisonService } from '../../../services/zone-livraison.service';
import { ZoneLivraisonDTO } from '../../../dto/zone-livraison.dto';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatGridListModule } from '@angular/material/grid-list';
import { MatToolbarModule } from '@angular/material/toolbar';

@Component({
  selector: 'app-zones-livraison-form',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatCheckboxModule,
    MatProgressBarModule,
    MatGridListModule,
    MatToolbarModule
  ],
  templateUrl: './zones-livraison-form.component.html',
  styleUrls: ['./zones-livraison-form.component.scss']
})
export class ZonesLivraisonFormComponent implements OnInit {
  zoneForm: FormGroup;
  isLoading = false;
  isEditMode = false;
  zoneId: number | null = null;
  errorMessage: string | null = null;
  successMessage: string | null = null;

  constructor(
    private fb: FormBuilder,
    private zoneLivraisonService: ZoneLivraisonService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.zoneForm = this.fb.group({
      nom: ['', [Validators.required, Validators.maxLength(100)]],
      code: ['', [Validators.required, Validators.maxLength(20)]],
      description: [''],
      fraisLivraison: [0, [Validators.required, Validators.min(0)]],
      delaiMinHeures: [24, [Validators.required, Validators.min(0)]],
      delaiMaxHeures: [72, [Validators.required, Validators.min(0)]],
      estActive: [true]
    }, { validators: this.delaiCoherentValidator });
  }

  ngOnInit(): void {
    // Check if we're in edit mode
    this.route.paramMap.subscribe(params => {
      const idParam = params.get('id');
      if (idParam) {
        this.isEditMode = true;
        this.zoneId = +idParam;
        this.loadZone(this.zoneId);
      }
    });
  }

  /**
   * Le délai maximal ne peut pas être inférieur au minimal.
   * La même règle est appliquée côté serveur (contrainte + validation).
   */
  private delaiCoherentValidator = (group: FormGroup) => {
    const min = group.get('delaiMinHeures')?.value;
    const max = group.get('delaiMaxHeures')?.value;
    if (min !== null && max !== null && min !== '' && max !== '' && Number(max) < Number(min)) {
      return { delaiIncoherent: true };
    }
    return null;
  };

  loadZone(id: number): void {
    this.isLoading = true;
    this.errorMessage = null;

    this.zoneLivraisonService.getZoneById(id).subscribe({
      next: (zone) => {
        this.zoneForm.patchValue(zone);
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading zone:', error);
        this.errorMessage = 'Impossible de charger cette zone.';
        this.isLoading = false;
      }
    });
  }

  onSubmit(): void {
    if (this.zoneForm.invalid) {
      this.zoneForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.errorMessage = null;
    this.successMessage = null;

    const raw = this.zoneForm.getRawValue();
    const zoneData: ZoneLivraisonDTO = {
      nom: raw.nom,
      code: raw.code,
      description: raw.description,
      fraisLivraison: Number(raw.fraisLivraison) || 0,
      delaiMinHeures: Number(raw.delaiMinHeures),
      delaiMaxHeures: Number(raw.delaiMaxHeures),
      estActive: !!raw.estActive
    };

    const onDone = () => {
      this.isLoading = false;
      this.router.navigate(['../']);
    };
    const onError = (error: { error?: { error?: string } }) => {
      console.error('Error saving zone:', error);
      this.errorMessage = error?.error?.error ?? 'Erreur lors de l\'enregistrement.';
      this.isLoading = false;
    };

    if (this.isEditMode && this.zoneId !== null) {
      this.zoneLivraisonService.updateZone(this.zoneId, zoneData).subscribe({ next: onDone, error: onError });
    } else {
      this.zoneLivraisonService.createZone(zoneData).subscribe({ next: onDone, error: onError });
    }
  }

  get hasDelaiError(): boolean {
    return this.zoneForm.hasError('delaiIncoherent');
  }

  // Helper to get form controls easily
  get f() {
    return this.zoneForm.controls;
  }
}