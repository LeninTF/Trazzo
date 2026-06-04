import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Planes } from './planes';

describe('Planes', () => {
  let component: Planes;
  let fixture: ComponentFixture<Planes>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Planes],
    }).compileComponents();

    fixture = TestBed.createComponent(Planes);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
