import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DirectorioPersonal } from './directorio-personal';

describe('DirectorioPersonal', () => {
  let component: DirectorioPersonal;
  let fixture: ComponentFixture<DirectorioPersonal>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DirectorioPersonal],
    }).compileComponents();

    fixture = TestBed.createComponent(DirectorioPersonal);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
