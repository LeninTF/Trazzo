import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-pagination',
  imports: [],
  template: `
    @if (totalPages > 1) {
      <nav>
        <ul class="pagination">
          <li class="page-item" [class.disabled]="currentPage <= 1">
            <a class="page-link" (click)="goTo(currentPage - 1)">
              <i class="bi bi-chevron-left"></i>
            </a>
          </li>
          @for (p of pages; track p) {
            <li class="page-item" [class.active]="p === currentPage">
              <a class="page-link" (click)="goTo(p)">{{ p }}</a>
            </li>
          }
          <li class="page-item" [class.disabled]="currentPage >= totalPages">
            <a class="page-link" (click)="goTo(currentPage + 1)">
              <i class="bi bi-chevron-right"></i>
            </a>
          </li>
        </ul>
      </nav>
    }
  `,
  styles: [`
    .pagination { margin: 0; }
  `]
})
export class PaginationComponent {
  @Input() currentPage = 1;
  @Input() totalPages = 1;
  @Output() pageChange = new EventEmitter<number>();

  get pages(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i + 1);
  }

  goTo(page: number): void {
    if (page >= 1 && page <= this.totalPages && page !== this.currentPage) {
      this.pageChange.emit(page);
    }
  }
}
