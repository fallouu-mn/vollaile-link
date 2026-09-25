import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { RequestService } from '../../services/request.service';
import { RequestDTO } from '../../dto/request.dto';

@Component({
  selector: 'app-request',
  templateUrl: './request.component.html',
  styleUrls: ['./request.component.scss'],
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule]
})
export class RequestComponent implements OnInit {
  readonly clientTypes = [
    { value: 'RESTAURANT', label: 'Restaurant' },
    { value: 'HOTEL', label: 'Hôtel' },
    { value: 'TRAITEUR', label: 'Traiteur' },
    { value: 'FAST_FOOD', label: 'Fast-food' },
    { value: 'REVENDEUR', label: 'Revendeur' },
    { value: 'BOUTIQUE', label: 'Boutique' },
    { value: 'PARTICULIER', label: 'Particulier' },
    { value: 'EVENEMENT', label: 'Organisateur d’événement' },
    { value: 'ASSOCIATION', label: 'Association' },
    { value: 'AUTRE', label: 'Autre' }
  ];

  readonly deliveryZones = [
    'Dakar',
    'Rufisque',
    'Thiès',
    'Mbour',
    'Diamniadio',
    'Pikine',
    'Guédiawaye',
    'Autre'
  ];

  readonly contactPreferences = [
    { value: 'TELEPHONE', label: 'Téléphone' },
    { value: 'WHATSAPP', label: 'WhatsApp' },
    { value: 'EMAIL', label: 'Email' }
  ];

  requestForm: FormGroup;
  isSubmitting = false;
  submitSuccess = false;
  submitError = false;
  errorMessage: string | null = null;
  offreId: number | null = null;

  constructor(
    private fb: FormBuilder,
    private requestService: RequestService,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.requestForm = this.fb.group({
      nom: ['', [Validators.required, Validators.minLength(2)]],
      prenom: ['', [Validators.required, Validators.minLength(2)]],
      telephone: ['', [Validators.required, Validators.pattern(/^\+221[0-9]{8,9}$/)]],
      email: ['', [Validators.required, Validators.email]],
      typeClient: ['RESTAURANT', Validators.required],
      zoneLivraison: ['DAKAR', Validators.required],
      nomProduit: ['', [Validators.required, Validators.minLength(2)]],
      quantiteSouhaitee: [null, [Validators.required, Validators.min(1)]],
      poidsSouhaite: [null, Validators.min(0.01)],
      dateSouhaitee: ['', Validators.required],
      adresseLivraison: [''],
      preferenceContact: ['TELEPHONE'],
      message: [''],
      honeypot: [''],
      consentement: [false, Validators.requiredTrue]
    });
  }

  get today(): string {
    return new Date().toISOString().slice(0, 10);
  }

  ngOnInit(): void {
    const rawOfferId = this.route.snapshot.queryParamMap.get('offreId');
    const parsedOfferId = Number(rawOfferId);
    if (rawOfferId && Number.isInteger(parsedOfferId) && parsedOfferId > 0) {
      this.offreId = parsedOfferId;
    }
  }

  onSubmit(): void {
    this.requestForm.markAllAsTouched();

    if (this.requestForm.invalid) {
      return;
    }

    const honeypotValue = this.requestForm.get('honeypot')?.value;
    if (honeypotValue && honeypotValue.trim() !== '') {
      this.submitError = true;
      this.errorMessage = 'Une erreur est survenue. Veuillez réessayer plus tard.';
      return;
    }

    this.isSubmitting = true;
    this.submitError = false;
    this.submitSuccess = false;
    this.errorMessage = null;

    const formData = this.requestForm.getRawValue();
    const requestData: RequestDTO = {
      nom: formData.nom,
      prenom: formData.prenom,
      telephone: formData.telephone,
      email: formData.email,
      typeClient: formData.typeClient,
      zoneLivraison: formData.zoneLivraison,
      nomProduit: formData.nomProduit,
      quantiteSouhaitee: Number(formData.quantiteSouhaitee),
      poidsSouhaite: formData.poidsSouhaite ? Number(formData.poidsSouhaite) : undefined,
      dateSouhaitee: formData.dateSouhaitee,
      adresseLivraison: formData.adresseLivraison || undefined,
      preferenceContact: formData.preferenceContact || undefined,
      message: formData.message || undefined,
      offreId: this.offreId ?? undefined,
      consentement: formData.consentement === true
    };

    this.requestService.submitRequest(requestData).subscribe({
      next: (acknowledgement) => {
        this.isSubmitting = false;
        this.submitSuccess = true;
        this.router.navigate(['/confirmation-demande'], {
          queryParams: { reference: acknowledgement.reference }
        });
      },
      error: (err) => {
        console.error('Error submitting request:', err);
        this.isSubmitting = false;
        this.submitError = true;
        if (err.status === 429) {
          this.errorMessage = 'Trop de demandes. Veuillez attendre quelques minutes avant de réessayer.';
        } else if (err.status === 400 && err.error?.details) {
          this.errorMessage = 'Certaines informations sont invalides. Vérifiez le formulaire.';
        } else {
          this.errorMessage = 'Une erreur est survenue. Veuillez réessayer plus tard.';
        }
      }
    });
  }
}