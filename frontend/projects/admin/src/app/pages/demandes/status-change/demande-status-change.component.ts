import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { DemandeService } from '../../../services/demande.service';
import { DemandeDTO } from '../../../dto/demande.dto';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatGridListModule } from '@angular/material/grid-list';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { MatToolbarModule } from '@angular/material/toolbar';

@Component({
  selector: 'app-demande-status-change',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    MatToolbarModule,
    MatGridListModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatProgressBarModule
  ],
  templateUrl: './demande-status-change.component.html',
  styleUrls: ['./demande-status-change.component.scss']
})
export class DemandeStatusChangeComponent implements OnInit {
  statusForm: FormGroup;
  demandeId: number | null = null;
  isLoading = false;
  demande: DemandeDTO | null = null;

  constructor(
    private fb: FormBuilder,
    private demandeService: DemandeService,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.statusForm = this.fb.group({
      statut: ['', Validators.required],
      commentairesAdmin: ['']
    });
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.demandeId = +id;
      this.loadDemande(this.demandeId);
    }
  }

  loadDemande(id: number): void {
    this.isLoading = true;
    this.demandeService.getDemandeById(id).subscribe({
      next: (demande) => {
        this.demande = demande;
        this.statusForm.patchValue({
          statut: demande.statut
        });
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading demande:', error);
        this.isLoading = false;
      }
    });
  }

  onSubmit(): void {
    if (this.statusForm.valid && this.demandeId) {
      this.isLoading = true;
      const { statut, commentairesAdmin } = this.statusForm.value;
      this.demandeService.updateStatut(this.demandeId, statut, commentairesAdmin).subscribe({
        next: (updatedDemande) => {
          this.isLoading = false;
          this.router.navigate(['/demandes']);
        },
        error: (error) => {
          console.error('Error updating demande status:', error);
          this.isLoading = false;
        }
      });
    }
  }

  // Helper to get form controls easily
  get f() {
    return this.statusForm.controls;
  }
}