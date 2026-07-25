import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';
import { Shop } from './shop';
import { SaasService } from '../../api/services/saas.service';
import { RedirectService } from '../../services/redirect.service';
import type { SaasPlanResult } from '../../api/types';

describe('Shop', () => {
  let component: Shop;
  let fixture: ComponentFixture<Shop>;

  const plan = (id: number, name: string, price: number): SaasPlanResult => ({
    id, name, price, priceAnnual: price * 10, currency: 'SOLES', billingPeriod: 'MONTHLY',
    active: true, createdAt: '2026-01-01T00:00:00', features: {},
  });

  const mockSaas = {
    listPublicPlans: jasmine.createSpy('listPublicPlans').and.returnValue(of([])),
    checkout: jasmine.createSpy('checkout').and.returnValue(of({ tenantId: 't-1', subDomain: 'acme', initPoint: 'https://mp/init' })),
  };
  const mockRedirect = { redirectTo: jasmine.createSpy('redirectTo') };

  const activatedRoute = (planId: string | null) => ({
    snapshot: { queryParamMap: { get: (_key: string) => planId } },
  });

  async function setup(planId: string | null = '2', plans: SaasPlanResult[] = [plan(2, 'Plan Demo', 29.99)]) {
    TestBed.resetTestingModule();
    mockSaas.listPublicPlans.calls.reset();
    mockSaas.checkout.calls.reset();
    mockRedirect.redirectTo.calls.reset();
    mockSaas.listPublicPlans.and.returnValue(of(plans));
    mockSaas.checkout.and.returnValue(of({ tenantId: 't-1', subDomain: 'acme', initPoint: 'https://mp/init' }));

    await TestBed.configureTestingModule({
      imports: [Shop],
      providers: [
        { provide: SaasService, useValue: mockSaas },
        { provide: ActivatedRoute, useValue: activatedRoute(planId) },
        { provide: RedirectService, useValue: mockRedirect },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Shop);
    component = fixture.componentInstance;
    await fixture.whenStable();
  }

  beforeEach(async () => {
    await setup();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load the plan matching the planId query param', () => {
    expect(component['plan']()?.id).toBe(2);
  });

  it('should fall back to the first plan when planId is missing', async () => {
    await setup(null, [plan(3, 'Plan Pro', 59.99)]);
    expect(component['plan']()?.id).toBe(3);
  });

  it('should start at section 1', () => {
    expect(component['activeSection']()).toBe(1);
  });

  it('should have initial form state', () => {
    const state = component['formState']();
    expect(state.firstName).toBe('');
    expect(state.terms).toBeFalse();
  });

  it('should compute completedFields', () => {
    expect(component['completedFields']()).toBe(0);
    component['formState'].update(s => ({ ...s, firstName: 'John', terms: true }));
    expect(component['completedFields']()).toBe(2);
  });

  it('should compute progressPercent', () => {
    expect(component['progressPercent']()).toBe(0);
    component['formState'].update(s => ({ ...s, firstName: 'John', terms: true }));
    const pct = Math.round((2 / 13) * 100);
    expect(component['progressPercent']()).toBe(pct);
  });

  it('should compute completionLabel', () => {
    expect(component['completionLabel']()).toBe('0 de 13 campos completados');
    component['formState'].update(s => ({ ...s, anotherAdmin: true }));
    expect(component['completionLabel']()).toContain('20');
  });

  it('should setSection', () => {
    component['setSection'](2);
    expect(component['activeSection']()).toBe(2);
  });

  it('should updateTextField', () => {
    const event = { target: { value: 'John' } } as unknown as Event;
    component['updateTextField']('firstName', event);
    expect(component['formState']().firstName).toBe('John');
  });

  it('should updateCheckboxField for anotherAdmin', () => {
    const event = { target: { checked: true } } as unknown as Event;
    component['updateCheckboxField']('anotherAdmin', event);
    expect(component['formState']().anotherAdmin).toBeTrue();
    expect(component['activeSection']()).toBe(4);
  });

  it('should updateCheckboxField for terms', () => {
    const event = { target: { checked: true } } as unknown as Event;
    component['updateCheckboxField']('terms', event);
    expect(component['formState']().terms).toBeTrue();
  });

  it('should onSectionToggle set section when opening', () => {
    const details = { open: true } as HTMLDetailsElement;
    const event = { target: details } as unknown as Event;
    component['onSectionToggle'](2, event);
    expect(component['activeSection']()).toBe(2);
  });

  it('should onSectionToggle set section to 0 when closing active section', () => {
    component['setSection'](2);
    const details = { open: false } as HTMLDetailsElement;
    const event = { target: details } as unknown as Event;
    component['onSectionToggle'](2, event);
    expect(component['activeSection']()).toBe(0);
  });

  describe('submitCheckout', () => {
    function submitEvent(): Event {
      return { preventDefault: jasmine.createSpy('preventDefault') } as unknown as Event;
    }

    it('should reject submission when terms are not accepted', () => {
      component['submitCheckout'](submitEvent());
      expect(component['errorMessage']()).toContain('términos');
      expect(mockSaas.checkout).not.toHaveBeenCalled();
    });

    it('should call checkout and redirect on success', () => {
      component['formState'].update(s => ({ ...s, terms: true }));
      component['submitCheckout'](submitEvent());

      expect(mockSaas.checkout).toHaveBeenCalled();
      expect(mockRedirect.redirectTo).toHaveBeenCalledWith('https://mp/init');
    });

    it('should show an error message when checkout fails', () => {
      mockSaas.checkout.and.returnValue(throwError(() => new Error('fail')));
      component['formState'].update(s => ({ ...s, terms: true }));

      component['submitCheckout'](submitEvent());

      expect(component['errorMessage']()).toContain('No se pudo iniciar el pago');
      expect(component['submitting']()).toBeFalse();
    });
  });

  it('should load plan matching the provided planId query param', async () => {
    await setup('2', [plan(1, 'Basic', 9.99), plan(2, 'Pro', 49.99)]);
    expect(component['plan']()?.id).toBe(2);
  });

  it('should return early from onSectionToggle when target is null', () => {
    const event = { target: null } as unknown as Event;
    component['onSectionToggle'](2, event);
    expect(component['activeSection']()).toBe(1);
  });

  it('should not change activeSection when closing non-active section', () => {
    component['setSection'](1);
    const details = { open: false } as HTMLDetailsElement;
    const event = { target: details } as unknown as Event;
    component['onSectionToggle'](3, event);
    expect(component['activeSection']()).toBe(1);
  });

  it('should set activeSection to 3 when unchecking anotherAdmin', () => {
    component['formState'].update(s => ({ ...s, anotherAdmin: true }));
    const event = { target: { checked: false } } as unknown as Event;
    component['updateCheckboxField']('anotherAdmin', event);
    expect(component['formState']().anotherAdmin).toBeFalse();
    expect(component['activeSection']()).toBe(3);
  });

  it('should include admin fields in completedFields when anotherAdmin is true', () => {
    component['formState'].update(s => ({
      ...s, anotherAdmin: true,
      firstName: 'A', lastNamePaterno: 'B', lastNameMaterno: 'C',
      documentType: 'DNI', documentNumber: '1', email: 'e@e', phone: '9',
      ruc: '20', companyName: 'Co', businessName: 'Bu', address: 'Av',
      adminFirstName: 'X', adminLastNamePaterno: 'Y', adminLastNameMaterno: 'Z',
      adminDocumentType: 'DNI', adminDocumentNumber: '2', adminEmail: 'f@f', adminPhone: '8',
      terms: true,
    }));
    expect(component['completedFields']()).toBe(20);
  });

  it('should submit checkout with anotherAdmin fields when anotherAdmin is true', () => {
    component['formState'].update(s => ({
      ...s, terms: true, anotherAdmin: true,
      adminFirstName: 'Jane', adminLastNamePaterno: 'Doe', adminLastNameMaterno: 'M',
      adminDocumentType: 'DNI', adminDocumentNumber: '87654321',
      adminEmail: 'admin@test.com', adminPhone: '999',
    }));
    const evt = { preventDefault: jasmine.createSpy('preventDefault') } as unknown as Event;
    component['submitCheckout'](evt);
    expect(mockSaas.checkout).toHaveBeenCalled();
    const req = mockSaas.checkout.calls.mostRecent().args[0];
    expect(req.anotherAdmin).toBeTrue();
    expect(req.adminFirstName).toBe('Jane');
  });
});
