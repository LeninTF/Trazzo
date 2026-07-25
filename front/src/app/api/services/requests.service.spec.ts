import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { RequestsService } from './requests.service';
import { API_BASE_URL } from './helpers';

describe('RequestsService', () => {
  let service: RequestsService;
  let httpMock: HttpTestingController;
  let apiBase: string;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        RequestsService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: 'https://api.trazzo.pe/api/v1' },
      ],
    });
    service = TestBed.inject(RequestsService);
    httpMock = TestBed.inject(HttpTestingController);
    apiBase = TestBed.inject(API_BASE_URL);
  });

  afterEach(() => httpMock.verify());

  it('should submit a public request', () => {
    const body = { name: 'Test', email: 'a@b.com', message: 'Hello' } as any;
    service.submit(body).subscribe();
    const req = httpMock.expectOne(`${apiBase}/requests`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    req.flush({ id: 1 });
  });

  it('should list requests with no params', () => {
    service.list().subscribe();
    const req = httpMock.expectOne(r => r.url === `${apiBase}/saas/requests`);
    expect(req.request.method).toBe('GET');
    req.flush({ content: [], totalElements: 0 });
  });

  it('should list requests with all params', () => {
    service.list({ status: 'OPEN', type: 'BUG', search: 'test', page: 2, size: 10 }).subscribe();
    const req = httpMock.expectOne(r => r.url === `${apiBase}/saas/requests`);
    expect(req.request.params.get('status')).toBe('OPEN');
    expect(req.request.params.get('type')).toBe('BUG');
    expect(req.request.params.get('search')).toBe('test');
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('10');
    req.flush({ content: [], totalElements: 0 });
  });

  it('should get request by id', () => {
    service.getById(42).subscribe();
    const req = httpMock.expectOne(`${apiBase}/saas/requests/42`);
    expect(req.request.method).toBe('GET');
    req.flush({ id: 42 });
  });

  it('should change request status', () => {
    const body = { status: 'RESOLVED', comment: 'Fixed' } as any;
    service.changeStatus(5, body).subscribe();
    const req = httpMock.expectOne(`${apiBase}/saas/requests/5/status`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual(body);
    req.flush({ id: 5 });
  });

  it('should add comment', () => {
    service.addComment(10, 'Thanks').subscribe();
    const req = httpMock.expectOne(`${apiBase}/saas/requests/10/comments`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ comment: 'Thanks' });
    req.flush({ id: 1 });
  });
});
