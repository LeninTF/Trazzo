import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { Shop } from './shop';
import { SaasService } from '../../api/services/saas.service';
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
  };

  const activatedRoute = (planId: string | null) => ({
    snapshot: { queryParamMap: { get: (_key: string) => planId } },
  });

  async function setup(planId: string | null = '2', plans: SaasPlanResult[] = [plan(2, 'Plan Demo', 29.99)]) {
    TestBed.resetTestingModule();
    mockSaas.listPublicPlans.calls.reset();
    mockSaas.listPublicPlans.and.returnValue(of(plans));

    await TestBed.configureTestingModule({
      imports: [Shop],
      providers: [
        { provide: SaasService, useValue: mockSaas },
        { provide: ActivatedRoute, useValue: activatedRoute(planId) },
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
});
