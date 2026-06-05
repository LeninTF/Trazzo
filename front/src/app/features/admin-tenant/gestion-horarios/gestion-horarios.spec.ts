import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GestionHorarios } from './gestion-horarios';

describe('GestionHorarios', () => {
  let component: GestionHorarios;
  let fixture: ComponentFixture<GestionHorarios>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GestionHorarios],
    }).compileComponents();

    fixture = TestBed.createComponent(GestionHorarios);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
