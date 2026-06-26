import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TermsAndConditions } from './terms-and-conditions';

describe('TermsAndConditions', () => {
  let component: TermsAndConditions;
  let fixture: ComponentFixture<TermsAndConditions>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TermsAndConditions],
    }).compileComponents();

    fixture = TestBed.createComponent(TermsAndConditions);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
