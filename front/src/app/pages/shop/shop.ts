import { CommonModule, Location } from '@angular/common';
import { Component, computed, signal } from '@angular/core';

type PaymentFormState = {
  firstName: string;
  lastNamePaterno: string;
  lastNameMaterno: string;
  documentType: string;
  documentNumber: string;
  email: string;
  phone: string;
  adminFirstName: string;
  adminLastNamePaterno: string;
  adminLastNameMaterno: string;
  adminDocumentType: string;
  adminDocumentNumber: string;
  adminEmail: string;
  adminPhone: string;
  ruc: string;
  companyName: string;
  businessName: string;
  address: string;
  anotherAdmin: boolean;
  terms: boolean;
};

@Component({
  selector: 'app-shop',
  imports: [CommonModule],
  templateUrl: './shop.html',
  styleUrl: './shop.css',
})
export class Shop {
  constructor(private readonly location: Location) {}

  protected readonly activeSection = signal(1);

  protected readonly formState = signal<PaymentFormState>({
    firstName: '',
    lastNamePaterno: '',
    lastNameMaterno: '',
    documentType: '',
    documentNumber: '',
    email: '',
    phone: '',
    adminFirstName: '',
    adminLastNamePaterno: '',
    adminLastNameMaterno: '',
    adminDocumentType: '',
    adminDocumentNumber: '',
    adminEmail: '',
    adminPhone: '',
    ruc: '',
    companyName: '',
    businessName: '',
    address: '',
    anotherAdmin: false,
    terms: false,
  });

  protected readonly completedFields = computed(() => {
    const state = this.formState();
    const fields = [
      state.firstName,
      state.lastNamePaterno,
      state.lastNameMaterno,
      state.documentType,
      state.documentNumber,
      state.email,
      state.phone,
      state.ruc,
      state.companyName,
      state.businessName,
      state.address,
      state.anotherAdmin,
      state.terms,
    ];

    if (state.anotherAdmin) {
      fields.push(
        state.adminFirstName,
        state.adminLastNamePaterno,
        state.adminLastNameMaterno,
        state.adminDocumentType,
        state.adminDocumentNumber,
        state.adminEmail,
        state.adminPhone,
      );
    }

    return fields.filter(Boolean).length;
  });

  protected readonly progressPercent = computed(() => {
    const baseFields = 13;
    const adminFields = this.formState().anotherAdmin ? 7 : 0;
    const totalFields = baseFields + adminFields;

    return Math.round((this.completedFields() / totalFields) * 100);
  });

  protected readonly completionLabel = computed(
    () => {
      const totalFields = 13 + (this.formState().anotherAdmin ? 7 : 0);

      return `${this.completedFields()} de ${totalFields} campos completados`;
    },
  );

  protected setSection(section: number): void {
    this.activeSection.set(section);
  }

  protected goBack(): void {
    this.location.back();
  }

  protected onSectionToggle(section: number, event: Event): void {
    const target = event.target as HTMLDetailsElement | null;

    if (!target) {
      return;
    }

    if (target.open) {
      this.activeSection.set(section);
      return;
    }

    if (this.activeSection() === section) {
      this.activeSection.set(0);
    }
  }

  protected updateTextField(field: keyof PaymentFormState, event: Event): void {
    const target = event.target as HTMLInputElement | HTMLSelectElement | null;
    const value = target?.value ?? '';

    this.formState.update((current) => ({
      ...current,
      [field]: value,
    }));
  }

  protected updateCheckboxField(field: 'anotherAdmin' | 'terms', event: Event): void {
    const target = event.target as HTMLInputElement | null;
    const checked = target?.checked ?? false;

    this.formState.update((current) => ({
      ...current,
      [field]: checked,
    }));

    if (field === 'anotherAdmin') {
      this.activeSection.set(checked ? 4 : 3);
    }
  }
}
