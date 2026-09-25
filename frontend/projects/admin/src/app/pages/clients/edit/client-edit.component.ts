import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ClientService } from '../../../services/client.service';
import { ClientDTO } from '../../../dto/client.dto';
import { Router, ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-client-edit',
  standalone: false,
  templateUrl: './client-edit.component.html',
  styleUrls: ['./client-edit.component.scss']
})
export class ClientEditComponent implements OnInit {
  clientForm: FormGroup;
  isLoading = false;
  telephoneExistsError = false;
  clientId: number | null = null;

  constructor(
    private fb: FormBuilder,
    private clientService: ClientService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.clientForm = this.fb.group({
      nom: ['', Validators.required],
      prenom: ['', Validators.required],
      telephone: ['', [Validators.required, Validators.pattern(/^\+221\d{9}$/)]],
      email: ['', [Validators.email]],
      adresse: [''],
      ville: [''],
      region: [''],
      dateNaissance: [''],
      sexe: [''],
      professionnel: [false],
      entreprise: [''],
      secteurActif: [''],
      isActive: [true]
    });
  }

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.clientId = +id;
        this.loadClient(this.clientId);
      }
    });

    // Watch for changes to professionnel to enable/disable entreprise and secteurActif
    this.clientForm.get('professionnel')?.valueChanges.subscribe((isPro: boolean) => {
      const entrepriseCtrl = this.clientForm.get('entreprise');
      const secteurActifCtrl = this.clientForm.get('secteurActif');
      if (isPro) {
        entrepriseCtrl?.setValidators([Validators.required]);
        secteurActifCtrl?.setValidators([Validators.required]);
      } else {
        entrepriseCtrl?.clearValidators();
        secteurActifCtrl?.clearValidators();
        entrepriseCtrl?.setValue('');
        secteurActifCtrl?.setValue('');
      }
      entrepriseCtrl?.updateValueAndValidity();
      secteurActifCtrl?.updateValueAndValidity();
    });

    // Watch for telephone changes to check uniqueness (excluding current client)
    this.clientForm.get('telephone')?.valueChanges.subscribe((telephone: string) => {
      if (telephone && this.clientForm.get('telephone')?.valid && this.clientId) {
        // We would need a backend endpoint to check telephone excluding current client
        // For now, we'll just check if it exists (and if it's the same as current, it's okay)
        this.clientService.telephoneExists(telephone).subscribe((exists) => {
          // If it exists and it's not the current client, show error
          if (exists && this.clientId) {
            this.clientService.getClientById(this.clientId).subscribe((currentClient) => {
              this.telephoneExistsError = currentClient.telephone !== telephone;
            });
          } else {
            this.telephoneExistsError = false;
          }
        });
      } else {
        this.telephoneExistsError = false;
      }
    });
  }

  loadClient(id: number): void {
    this.isLoading = true;
    this.clientService.getClientById(id).subscribe({
      next: (client) => {
        // Format dateNaissance for the input
        const clientData: ClientDTO = {
          ...client,
          dateNaissance: client.dateNaissance ? new Date(client.dateNaissance).toISOString().split('T')[0] : undefined
        };
        this.clientForm.patchValue(clientData);
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading client:', error);
        this.isLoading = false;
      }
    });
  }

  onSubmit(): void {
    if (this.clientForm.valid && !this.telephoneExistsError && this.clientId) {
      this.isLoading = true;
      const clientData: ClientDTO = this.clientForm.value;
      // Le contrôle utilise une date ISO déjà sous forme de chaîne.
      if (typeof clientData.dateNaissance === 'string' && clientData.dateNaissance) {
        clientData.dateNaissance = clientData.dateNaissance.split('T')[0];
      }
      this.clientService.updateClient(this.clientId, clientData).subscribe({
        next: (updatedClient) => {
          this.isLoading = false;
          this.router.navigate(['../']);
        },
        error: (error) => {
          console.error('Error updating client:', error);
          this.isLoading = false;
        }
      });
    }
  }

  // Helper to get form controls easily
  get f() {
    return this.clientForm.controls;
  }
}