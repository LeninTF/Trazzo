import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';

import { GestionRoles } from './gestion-roles';

describe('GestionRoles', () => {
  let component: GestionRoles;
  let fixture: ComponentFixture<GestionRoles>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GestionRoles, FormsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(GestionRoles);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
