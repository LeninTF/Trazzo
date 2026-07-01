import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PaginationComponent } from './pagination.component';

describe('PaginationComponent', () => {
  let component: PaginationComponent;
  let fixture: ComponentFixture<PaginationComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PaginationComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(PaginationComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have default currentPage as 1', () => {
    expect(component.currentPage).toBe(1);
  });

  it('should have default totalPages as 1', () => {
    expect(component.totalPages).toBe(1);
  });

  describe('pages', () => {
    it('should return array from 1 to totalPages', () => {
      component.totalPages = 5;
      expect(component.pages).toEqual([1, 2, 3, 4, 5]);
    });

    it('should return empty array when totalPages is 0', () => {
      component.totalPages = 0;
      expect(component.pages).toEqual([]);
    });
  });

  describe('goTo', () => {
    it('should emit pageChange when page is valid', () => {
      spyOn(component.pageChange, 'emit');
      component.totalPages = 5;
      component.goTo(3);
      expect(component.pageChange.emit).toHaveBeenCalledWith(3);
    });

    it('should not emit when page is below 1', () => {
      spyOn(component.pageChange, 'emit');
      component.goTo(0);
      expect(component.pageChange.emit).not.toHaveBeenCalled();
    });

    it('should not emit when page exceeds totalPages', () => {
      spyOn(component.pageChange, 'emit');
      component.totalPages = 5;
      component.goTo(6);
      expect(component.pageChange.emit).not.toHaveBeenCalled();
    });

    it('should not emit when page is the same as currentPage', () => {
      spyOn(component.pageChange, 'emit');
      component.totalPages = 5;
      component.goTo(1);
      expect(component.pageChange.emit).not.toHaveBeenCalled();
    });
  });

  describe('template', () => {
    it('should render pagination when totalPages > 1', () => {
      component.totalPages = 3;
      fixture.detectChanges();
      const nav: HTMLElement = fixture.nativeElement.querySelector('nav');
      expect(nav).toBeTruthy();
    });

    it('should not render pagination when totalPages is 1', () => {
      fixture.detectChanges();
      const nav: HTMLElement = fixture.nativeElement.querySelector('nav');
      expect(nav).toBeFalsy();
    });

    it('should highlight active page', () => {
      component.totalPages = 3;
      component.currentPage = 2;
      fixture.detectChanges();

      const activeItem: HTMLElement = fixture.nativeElement.querySelector('.page-item.active');
      expect(activeItem).toBeTruthy();
      expect(activeItem.textContent?.trim()).toBe('2');
    });

    it('should disable previous button on first page', () => {
      component.totalPages = 3;
      component.currentPage = 1;
      fixture.detectChanges();

      const items: NodeListOf<HTMLElement> = fixture.nativeElement.querySelectorAll('.page-item');
      expect(items[0].classList.contains('disabled')).toBeTrue();
    });

    it('should disable next button on last page', () => {
      component.totalPages = 3;
      component.currentPage = 3;
      fixture.detectChanges();

      const items: NodeListOf<HTMLElement> = fixture.nativeElement.querySelectorAll('.page-item');
      expect(items[items.length - 1].classList.contains('disabled')).toBeTrue();
    });

    it('should call goTo when page link is clicked', () => {
      spyOn(component, 'goTo');
      component.totalPages = 3;
      fixture.detectChanges();

      const pageLinks: NodeListOf<HTMLElement> = fixture.nativeElement.querySelectorAll('.page-link');
      (pageLinks[1] as HTMLElement).click();
      expect(component.goTo).toHaveBeenCalledWith(1);
    });
  });
});
