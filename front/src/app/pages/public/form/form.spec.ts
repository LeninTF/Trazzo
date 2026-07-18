import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { Form } from './form';
import { ApiService } from '../../../api/services/api.service';
import { ToastService } from '../../../services/toast.service';

describe('Form', () => {
  let component: Form;
  let fixture: ComponentFixture<Form>;
  let mockRequests: { submit: jasmine.Spy };
  let mockToast: jasmine.SpyObj<ToastService>;

  const fillValidForm = () => {
    component.names.set('Ana');
    component.lastNames.set('Perez');
    component.email.set('ana@example.com');
    component.phone.set('999999999');
    component.interestType.set('trial');
    component.ruc.set('20123456789');
    component.businessName.set('Acme SAC');
    component.message.set('Quiero una demo');
  };

  beforeEach(async () => {
    mockRequests = { submit: jasmine.createSpy('submit').and.returnValue(of({ id: 1 })) };
    mockToast = jasmine.createSpyObj('ToastService', ['show', 'success', 'error', 'info']);

    await TestBed.configureTestingModule({
      imports: [Form],
      providers: [
        { provide: ApiService, useValue: { requests: mockRequests } },
        { provide: ToastService, useValue: mockToast },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Form);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show an error toast and not submit when fields are missing', () => {
    const event = new Event('submit');
    component.onSubmit(event);

    expect(mockToast.error).toHaveBeenCalledWith('Por favor, completa todos los campos');
    expect(mockRequests.submit).not.toHaveBeenCalled();
  });

  it('should submit and show a success toast when the form is valid', () => {
    fillValidForm();
    const event = new Event('submit');

    component.onSubmit(event);

    expect(mockRequests.submit).toHaveBeenCalledWith({
      type: 'trial', name: 'Ana', lastName: 'Perez', email: 'ana@example.com',
      phoneNumber: '999999999', taxId: '20123456789', companyName: 'Acme SAC', message: 'Quiero una demo',
    });
    expect(component.submitted()).toBeTrue();
    expect(component.isLoading()).toBeFalse();
    expect(mockToast.success).toHaveBeenCalled();
  });

  it('should show the rate-limit message on a 429 response', () => {
    mockRequests.submit.and.returnValue(throwError(() => ({ status: 429, error: { message: 'Ya enviaste una solicitud' } })));
    fillValidForm();

    component.onSubmit(new Event('submit'));

    expect(component.isLoading()).toBeFalse();
    expect(mockToast.error).toHaveBeenCalledWith('Ya enviaste una solicitud');
  });

  it('should show a generic error message on other failures', () => {
    mockRequests.submit.and.returnValue(throwError(() => ({ status: 500 })));
    fillValidForm();

    component.onSubmit(new Event('submit'));

    expect(mockToast.error).toHaveBeenCalledWith('No se pudo enviar la solicitud. Intenta nuevamente.');
  });
});
