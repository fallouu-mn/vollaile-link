import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ClientService } from '../../../services/client.service';
import { ClientDTO } from '../../../dto/client.dto';
import { Router } from '@angular/router';

@Component({
  selector: 'app-client-create',
  standalone: false,
  templateUrl: './client-create.component.html',
  styleUrls: ['./client-create.component.scss']
})
export class ClientCreateComponent implements OnInit {
  clientForm: FormGroup;
  isLoading = false;
  telephoneExistsError = false;

  constructor(
    private fb: FormBuilder,
    private clientService: ClientService,
    private router: Router
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

    // Watch for telephone changes to check uniqueness
    this.clientForm.get('telephone')?.valueChanges.subscribe((telephone: string) => {
      if (telephone && this.clientForm.get('telephone')?.valid) {
        this.clientService.telephoneExists(telephone).subscribe((exists) => {
          this.telephoneExistsError = exists;
        });
      } else {
        this.telephoneExistsError = false;
      }
    });
  }

  onSubmit(): void {
    if (this.clientForm.valid && !this.telephoneExistsError) {
      this.isLoading = true;
      const clientData: ClientDTO = this.clientForm.value;
      // Le contrôle utilise une date ISO déjà sous forme de chaîne.
      if (typeof clientData.dateNaissance === 'string' && clientData.dateNaissance) {
        clientData.dateNaissance = clientData.dateNaissance.split('T')[0];
      }
      this.clientService.createClient(clientData).subscribe({
        next: (createdClient) => {
          this.isLoading = false;
          this.router.navigate(['../']);
        },
        error: (error) => {
          console.error('Error creating client:', error);
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