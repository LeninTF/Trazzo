import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LogAuditoria } from './log-auditoria';

describe('LogAuditoria', () => {
  let component: LogAuditoria;
  let fixture: ComponentFixture<LogAuditoria>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LogAuditoria],
    }).compileComponents();

    fixture = TestBed.createComponent(LogAuditoria);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
