import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Incidencias } from './incidencias';

describe('Incidencias', () => {
  let component: Incidencias;
  let fixture: ComponentFixture<Incidencias>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Incidencias],
    }).compileComponents();

    fixture = TestBed.createComponent(Incidencias);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
