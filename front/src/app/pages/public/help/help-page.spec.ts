import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject } from 'rxjs';
import { provideLocationMocks } from '@angular/common/testing';
import { provideRouter, ActivatedRoute } from '@angular/router';
import { HelpPage } from './help-page';
import { helpContent } from './help.data';

describe('HelpPage', () => {
  let component: HelpPage;
  let fixture: ComponentFixture<HelpPage>;
  let paramMapSubject: Subject<{ get: (key: string) => string | null }>;

  beforeEach(async () => {
    paramMapSubject = new Subject();

    await TestBed.configureTestingModule({
      imports: [HelpPage],
      providers: [
        provideRouter([]),
        provideLocationMocks(),
        { provide: ActivatedRoute, useValue: { paramMap: paramMapSubject.asObservable() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(HelpPage);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
    expect(component['sections'].length).toBeGreaterThan(0);
  });

  it('should have content as first help section by default', () => {
    expect(component['content'].id).toBe('guia-de-uso');
  });

  it('should have sections from helpContent', () => {
    expect(component['sections'].length).toBe(3);
    expect(component['sections']).toBe(helpContent);
  });

  it('should update content when route param changes', () => {
    paramMapSubject.next({ get: () => 'soporte-tecnico' });
    expect(component['content'].id).toBe('soporte-tecnico');
  });

  it('should set prev null and next to second for first section', () => {
    paramMapSubject.next({ get: () => 'guia-de-uso' });
    expect(component['prev']).toBeNull();
    expect(component['next']?.id).toBe('soporte-tecnico');
  });

  it('should set prev and next for middle section', () => {
    paramMapSubject.next({ get: () => 'soporte-tecnico' });
    expect(component['prev']?.id).toBe('guia-de-uso');
    expect(component['next']?.id).toBe('acerca-de-trazzo');
  });

  it('should set prev to second-to-last and next null for last section', () => {
    paramMapSubject.next({ get: () => 'acerca-de-trazzo' });
    expect(component['prev']?.id).toBe('soporte-tecnico');
    expect(component['next']).toBeNull();
  });

  it('should not update content for unknown section id', () => {
    paramMapSubject.next({ get: () => 'non-existent' });
    expect(component['content'].id).toBe('guia-de-uso');
    expect(component['prev']).toBeNull();
    expect(component['next']).toBeNull();
  });
});
