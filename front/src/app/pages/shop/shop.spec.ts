import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Shop } from './shop';

describe('Shop', () => {
  let component: Shop;
  let fixture: ComponentFixture<Shop>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Shop],
    }).compileComponents();

    fixture = TestBed.createComponent(Shop);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
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
