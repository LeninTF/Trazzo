import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { Index } from './index';
import { SaasService } from '../../../api/services/saas.service';
import type { SaasPlanResult } from '../../../api/types';

describe('Index', () => {
  let component: Index;
  let fixture: ComponentFixture<Index>;

  const plan = (id: number, name: string, price: number, features: Record<string, number | boolean> = {}): SaasPlanResult => ({
    id, name, price, priceAnnual: price * 10, currency: 'SOLES', billingPeriod: 'MONTHLY',
    active: true, createdAt: '2026-01-01T00:00:00', features,
  });

  const mockSaas = {
    listPublicPlans: jasmine.createSpy('listPublicPlans').and.returnValue(of([])),
  };

  beforeEach(async () => {
    mockSaas.listPublicPlans.calls.reset();
    mockSaas.listPublicPlans.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [Index],
      providers: [{ provide: SaasService, useValue: mockSaas }],
    }).compileComponents();

    fixture = TestBed.createComponent(Index);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load public plans without authentication', () => {
    expect(mockSaas.listPublicPlans).toHaveBeenCalled();
  });

  it('should show no plan cards when there are none', () => {
    expect(component.planCards()).toEqual([]);
  });

  it('should sort plans by price and mark the middle one as destacado for 3 plans', () => {
    mockSaas.listPublicPlans.and.returnValue(of([
      plan(1, 'Enterprise', 499),
      plan(2, 'Starter', 49),
      plan(3, 'Professional', 149),
    ]));
    fixture = TestBed.createComponent(Index);
    component = fixture.componentInstance;

    const cards = component.planCards();
    expect(cards.map(c => c.plan.name)).toEqual(['Starter', 'Professional', 'Enterprise']);
    expect(cards[1].destacado).toBeTrue();
    expect(cards[0].destacado).toBeFalse();
    expect(cards[2].destacado).toBeFalse();
  });

  it('should not mark any plan as destacado when there is only one', () => {
    mockSaas.listPublicPlans.and.returnValue(of([plan(1, 'Solo', 99)]));
    fixture = TestBed.createComponent(Index);
    component = fixture.componentInstance;

    expect(component.planCards()[0].destacado).toBeFalse();
  });

  it('should build human-readable feature labels', () => {
    mockSaas.listPublicPlans.and.returnValue(of([
      plan(1, 'Basico', 49, { max_trabajadores: 5, max_sedes: 1, almacenamiento_gb: 10, 'soporte-24-7': true, reportes: false }),
    ]));
    fixture = TestBed.createComponent(Index);
    component = fixture.componentInstance;

    const features = component.planCards()[0].features;
    expect(features).toContain('Hasta 5 usuarios');
    expect(features).toContain('Hasta 1 sedes');
    expect(features).toContain('10GB de almacenamiento');
    expect(features).toContain('Soporte 24/7');
    expect(features).not.toContain('Reportes Avanzados');
  });

  it('should stop loading on error without throwing', () => {
    mockSaas.listPublicPlans.and.returnValue(throwError(() => new Error('fail')));
    fixture = TestBed.createComponent(Index);
    component = fixture.componentInstance;

    expect(component.loading()).toBeFalse();
    expect(component.planCards()).toEqual([]);
  });
});
