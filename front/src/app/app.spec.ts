import { TestBed } from '@angular/core/testing';
import { Router, NavigationEnd, ActivatedRoute } from '@angular/router';
import { Subject } from 'rxjs';
import { App } from './app';
import { RoleService } from './services/role.service';

describe('App', () => {
  let routerEvents: Subject<any>;
  let routerSpy: jasmine.SpyObj<Router>;
  let roleService: RoleService;

  beforeEach(async () => {
    routerEvents = new Subject<any>();
    routerSpy = jasmine.createSpyObj<Router>('Router', [], {
      url: '/login',
      events: routerEvents.asObservable(),
    });

    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        { provide: Router, useValue: routerSpy },
        { provide: ActivatedRoute, useValue: { snapshot: {} } },
        RoleService,
      ],
    }).compileComponents();

    roleService = TestBed.inject(RoleService);
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should initialize currentUrl with router.url', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app['currentUrl']()).toBe('/login');
  });

  it('should update currentUrl on NavigationEnd event', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;

    routerEvents.next(new NavigationEnd(1, '/old', '/new-path'));

    expect(app['currentUrl']()).toBe('/new-path');
  });

  it('should not update currentUrl on non-NavigationEnd events', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;

    routerEvents.next({ type: 'OtherEvent' });

    expect(app['currentUrl']()).toBe('/login');
  });

  it('should render app-toast component', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const toast: HTMLElement = fixture.nativeElement.querySelector('app-toast');
    expect(toast).toBeTruthy();
  });
});
