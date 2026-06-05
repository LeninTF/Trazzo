import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GestionPlanes } from './gestion-planes';

describe('GestionPlanes', () => {
  let component: GestionPlanes;
  let fixture: ComponentFixture<GestionPlanes>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GestionPlanes],
    }).compileComponents();

    fixture = TestBed.createComponent(GestionPlanes);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
