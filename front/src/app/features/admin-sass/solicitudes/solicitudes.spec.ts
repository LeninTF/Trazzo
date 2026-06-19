import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Solicitudes } from './solicitudes';

describe('Solicitudes', () => {
  let component: Solicitudes;
  let fixture: ComponentFixture<Solicitudes>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Solicitudes],
    }).compileComponents();

    fixture = TestBed.createComponent(Solicitudes);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
