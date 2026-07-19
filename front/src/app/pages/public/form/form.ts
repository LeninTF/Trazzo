import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ToastService } from '../../../services/toast.service';
import { ApiService } from '../../../api/services/api.service';

@Component({
  selector: 'app-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './form.html',
  styleUrl: './form.css',
})
export class Form {
  private readonly toastService = inject(ToastService);
  private readonly api = inject(ApiService);

  readonly names = signal('');
  readonly lastNames = signal('');
  readonly email = signal('');
  readonly phone = signal('');
  readonly interestType = signal('');
  readonly ruc = signal('');
  readonly businessName = signal('');
  readonly message = signal('');

  readonly isLoading = signal(false);
  readonly submitted = signal(false);

  onSubmit(event: Event): void {
    event.preventDefault();

    if (!this.names() || !this.lastNames() || !this.email() || !this.phone() || !this.interestType()
        || !this.ruc() || !this.businessName() || !this.message()) {
      this.toastService.error('Por favor, completa todos los campos');
      return;
    }

    this.isLoading.set(true);

    this.api.requests.submit({
      type: this.interestType() as 'trial' | 'info',
      name: this.names(),
      lastName: this.lastNames(),
      email: this.email(),
      phoneNumber: this.phone(),
      taxId: this.ruc(),
      companyName: this.businessName(),
      message: this.message(),
    }).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.submitted.set(true);
        this.toastService.success('Solicitud enviada. Te contactaremos pronto.');
      },
      error: (err) => {
        this.isLoading.set(false);
        const msg = err.status === 429
          ? (err.error?.message ?? 'Ya enviaste una solicitud recientemente. Intenta más tarde.')
          : 'No se pudo enviar la solicitud. Intenta nuevamente.';
        this.toastService.error(msg);
      },
    });
  }
}
