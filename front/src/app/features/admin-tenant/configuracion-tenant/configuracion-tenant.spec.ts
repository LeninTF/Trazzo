import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConfiguracionTenant } from './configuracion-tenant';

describe('ConfiguracionTenant', () => {
  let component: ConfiguracionTenant;
  let fixture: ComponentFixture<ConfiguracionTenant>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConfiguracionTenant],
    }).compileComponents();

    fixture = TestBed.createComponent(ConfiguracionTenant);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
