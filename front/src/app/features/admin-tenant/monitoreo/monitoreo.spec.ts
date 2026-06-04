import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Monitoreo } from './monitoreo';

describe('Monitoreo', () => {
  let component: Monitoreo;
  let fixture: ComponentFixture<Monitoreo>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Monitoreo],
    }).compileComponents();

    fixture = TestBed.createComponent(Monitoreo);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
