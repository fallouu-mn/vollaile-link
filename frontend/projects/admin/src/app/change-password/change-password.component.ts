import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';

@Component({
  selector: 'app-change-password',
  standalone: false,
  templateUrl: './change-password.component.html',
  styleUrls: ['./change-password.component.scss']
})
export class ChangePasswordComponent {
  changePasswordForm: FormGroup;
  loading = false;
  error = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.changePasswordForm = this.fb.group({
      currentPassword: ['', Validators.required],
      password: ['', [Validators.required, Validators.minLength(12)]],
      confirmPassword: ['', Validators.required]
    }, { validator: this.passwordMatchValidator });
  }

  passwordMatchValidator(g: FormGroup) {
    return g.get('password')?.value === g.get('confirmPassword')?.value
      ? null : { mismatch: true };
  }

  onSubmit() {
    if (this.changePasswordForm.invalid) {
      return;
    }

    this.loading = true;
    this.error = '';

    const currentPassword = this.changePasswordForm.get('currentPassword')?.value;
    const newPassword = this.changePasswordForm.get('password')?.value;

    this.authService.changePassword(currentPassword, newPassword)
      .subscribe({
        next: () => {
          this.loading = false;
          // Redirect to dashboard after successful password change
          this.router.navigate(['/dashboard']);
        },
        error: (err) => {
          this.loading = false;
          // Handle error - for now, show a generic message
          this.error = 'Erreur lors du changement de mot de passe';
          console.error('Password change error:', err);
        }
      });
  }

  // Getters for form controls
  get currentPassword() {
    return this.changePasswordForm.get('currentPassword');
  }

  get password() {
    return this.changePasswordForm.get('password');
  }

  get confirmPassword() {
    return this.changePasswordForm.get('confirmPassword');
  }
}
