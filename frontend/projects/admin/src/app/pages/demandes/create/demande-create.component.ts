import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { DemandeService } from '../../../services/demande.service';
import { DemandeDTO } from '../../../dto/demande.dto';
import { ClientService } from '../../../services/client.service';
import { ClientDTO } from '../../../dto/client.dto';
import { ProductService } from '../../../services/product.service';
import { ProductDTO } from '../../../dto/product.dto';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatGridListModule } from '@angular/material/grid-list';

@Component({
  selector: 'app-demande-create',
  templateUrl: './demande-create.component.html',
  styleUrls: ['./demande-create.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatGridListModule
  ]
})
export class DemandeCreateComponent implements OnInit {
  demandeForm: FormGroup;
  isLoading = false;
  clients: ClientDTO[] = [];
  produits: ProductDTO[] = [];

  constructor(
    private fb: FormBuilder,
    private demandeService: DemandeService,
    private clientService: ClientService,
    private productService: ProductService,
    private router: Router
  ) {
    this.demandeForm = this.fb.group({
      clientId: ['', Validators.required],
      produitId: [''],
      quantiteSouhaitee: [''],
      prixUnitaireSouhaite: [''],
      nomProduit: [''],
      description: [''],
      dateSouhaitee: [''],
      lieuLivraison: ['']
    });
  }

  ngOnInit(): void {
    this.loadClients();
    this.loadProduits();
  }

  loadClients(): void {
    this.clientService.getAllClients().subscribe({
      next: (clients) => {
        this.clients = clients;
      },
      error: (error) => {
        console.error('Error loading clients:', error);
      }
    });
  }

  loadProduits(): void {
    this.productService.getAllProducts().subscribe({
      next: (produits) => {
        this.produits = produits;
      },
      error: (error) => {
        console.error('Error loading produits:', error);
      }
    });
  }

  onSubmit(): void {
    if (this.demandeForm.valid) {
      this.isLoading = true;
      const rawValue = this.demandeForm.value;
      const demandeData: DemandeDTO = {
        ...rawValue,
        quantiteSouhaitee: rawValue.quantiteSouhaitee ? Number(rawValue.quantiteSouhaitee) : undefined,
        prixUnitaireSouhaite: rawValue.prixUnitaireSouhaite ? Number(rawValue.prixUnitaireSouhaite) : undefined,
        produitId: rawValue.produitId ? Number(rawValue.produitId) : undefined,
        dateSouhaitee: rawValue.dateSouhaitee || undefined,
        lieuLivraison: rawValue.lieuLivraison || undefined,
        nomProduit: rawValue.nomProduit || undefined,
        description: rawValue.description || undefined
      };

      this.demandeService.createDemande(demandeData).subscribe({
        next: (createdDemande) => {
          this.isLoading = false;
          this.router.navigate(['../']);
        },
        error: (error) => {
          console.error('Error creating demande:', error);
          this.isLoading = false;
        }
      });
    }
  }

  // Helper to get form controls easily
  get f() {
    return this.demandeForm.controls;
  }
}