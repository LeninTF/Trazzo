import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Tenants } from './tenants';

describe('Tenants', () => {
  let component: Tenants;
  let fixture: ComponentFixture<Tenants>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Tenants],
    }).compileComponents();

    fixture = TestBed.createComponent(Tenants);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
