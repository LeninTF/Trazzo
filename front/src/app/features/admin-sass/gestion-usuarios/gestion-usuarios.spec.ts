import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GestionUsuarios } from './gestion-usuarios';

describe('GestionUsuarios', () => {
  let component: GestionUsuarios;
  let fixture: ComponentFixture<GestionUsuarios>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GestionUsuarios],
    }).compileComponents();

    fixture = TestBed.createComponent(GestionUsuarios);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
