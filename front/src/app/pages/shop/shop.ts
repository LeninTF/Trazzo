import { CommonModule, Location } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { SaasService } from '../../api/services/saas.service';
import { RedirectService } from '../../services/redirect.service';
import type { SaasPlanResult, ShopCheckoutRequest } from '../../api/types';

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
  private readonly location = inject(Location);
  private readonly route = inject(ActivatedRoute);
  private readonly saasService = inject(SaasService);
  private readonly redirectService = inject(RedirectService);

  constructor(private readonly location: Location) {
    const planIdParam = this.route.snapshot.queryParamMap.get('planId');
    const planId = planIdParam ? Number(planIdParam) : null;
    this.saasService.listPublicPlans().subscribe({
      next: (plans) => {
        const match = planId != null ? plans.find((p) => p.id === planId) : undefined;
        this.plan.set(match ?? plans[0] ?? null);
      },
    });
  }

  protected readonly plan = signal<SaasPlanResult | null>(null);
  protected readonly activeSection = signal(1);
  protected readonly plan = signal<SaasPlanResult | null>(null);
  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  private readonly planId: number | null;

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

  constructor() {
    const planIdParam = this.route.snapshot.queryParamMap.get('planId');
    this.planId = planIdParam ? Number(planIdParam) : null;
    this.loadPlan();
  }

  private loadPlan(): void {
    this.saasService.listPublicPlans().subscribe({
      next: (plans) => {
        const match = this.planId != null ? plans.find((p) => p.id === this.planId) : undefined;
        this.plan.set(match ?? plans[0] ?? null);
      },
    });
  }

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

  protected submitCheckout(event: Event): void {
    event.preventDefault();
    this.errorMessage.set(null);

    const plan = this.plan();
    const state = this.formState();

    if (!plan) {
      this.errorMessage.set('No se pudo cargar el plan seleccionado.');
      return;
    }
    if (!state.terms) {
      this.errorMessage.set('Debes aceptar los términos y condiciones para continuar.');
      return;
    }

    const request: ShopCheckoutRequest = {
      planId: plan.id,
      firstName: state.firstName,
      lastNamePaterno: state.lastNamePaterno,
      lastNameMaterno: state.lastNameMaterno,
      documentType: state.documentType,
      documentNumber: state.documentNumber,
      email: state.email,
      phone: state.phone,
      ruc: state.ruc,
      companyName: state.companyName,
      businessName: state.businessName,
      address: state.address,
      anotherAdmin: state.anotherAdmin,
      ...(state.anotherAdmin
        ? {
            adminFirstName: state.adminFirstName,
            adminLastNamePaterno: state.adminLastNamePaterno,
            adminLastNameMaterno: state.adminLastNameMaterno,
            adminDocumentType: state.adminDocumentType,
            adminDocumentNumber: state.adminDocumentNumber,
            adminEmail: state.adminEmail,
            adminPhone: state.adminPhone,
          }
        : {}),
    };

    this.submitting.set(true);
    this.saasService.checkout(request).subscribe({
      next: (response) => {
        this.redirectService.redirectTo(response.initPoint);
      },
      error: () => {
        this.submitting.set(false);
        this.errorMessage.set('No se pudo iniciar el pago. Verifica los datos e inténtalo nuevamente.');
      },
    });
  }
}
