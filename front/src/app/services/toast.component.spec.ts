import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ToastComponent } from './toast.component';
import { ToastService } from './toast.service';

describe('ToastComponent', () => {
  let component: ToastComponent;
  let fixture: ComponentFixture<ToastComponent>;
  let toastService: ToastService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ToastComponent],
      providers: [ToastService],
    }).compileComponents();

    fixture = TestBed.createComponent(ToastComponent);
    component = fixture.componentInstance;
    toastService = TestBed.inject(ToastService);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should not render toast when toast is null', () => {
    const el: HTMLElement = fixture.nativeElement.querySelector('.toast-notification');
    expect(el).toBeFalsy();
  });

  it('should render notification when toast is set', () => {
    toastService.show('Test message', 'success');
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement.querySelector('.toast-notification');
    expect(el).toBeTruthy();
    expect(el.textContent).toContain('Test message');
  });

  it('should apply toast--success class for success type', () => {
    toastService.success('Success!');
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement.querySelector('.toast-notification');
    expect(el.classList.contains('toast--success')).toBeTrue();
  });

  it('should apply toast--error class for error type', () => {
    toastService.error('Error!');
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement.querySelector('.toast-notification');
    expect(el.classList.contains('toast--error')).toBeTrue();
  });

  it('should apply toast--info class for info type', () => {
    toastService.info('Info!');
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement.querySelector('.toast-notification');
    expect(el.classList.contains('toast--info')).toBeTrue();
  });

  it('should show correct icon for each type', () => {
    toastService.success('Success!');
    fixture.detectChanges();
    let icon: HTMLElement = fixture.nativeElement.querySelector('.toast-notification i');
    expect(icon.classList.contains('bi-check-circle-fill')).toBeTrue();

    toastService.error('Error!');
    fixture.detectChanges();
    icon = fixture.nativeElement.querySelector('.toast-notification i');
    expect(icon.classList.contains('bi-x-circle-fill')).toBeTrue();

    toastService.info('Info!');
    fixture.detectChanges();
    icon = fixture.nativeElement.querySelector('.toast-notification i');
    expect(icon.classList.contains('bi-info-circle-fill')).toBeTrue();
  });

  it('should hide notification when toast becomes null', () => {
    toastService.show('Temp');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.toast-notification')).toBeTruthy();

    toastService.toast.set(null);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.toast-notification')).toBeFalsy();
  });
});
