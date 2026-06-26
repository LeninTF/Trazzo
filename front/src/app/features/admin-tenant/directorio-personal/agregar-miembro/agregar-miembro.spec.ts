import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AgregarMiembro } from './agregar-miembro';

describe('AgregarMiembro', () => {
  let component: AgregarMiembro;
  let fixture: ComponentFixture<AgregarMiembro>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AgregarMiembro],
    }).compileComponents();

    fixture = TestBed.createComponent(AgregarMiembro);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
