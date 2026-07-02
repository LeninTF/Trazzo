import { Component, inject } from '@angular/core';
import { Location } from '@angular/common';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { helpContent, HelpSection } from './help.data';

@Component({
  selector: 'app-help-page',
  imports: [RouterLink],
  templateUrl: './help-page.html',
  styleUrl: './help-page.css',
})
export class HelpPage {
  private readonly route = inject(ActivatedRoute);
  protected readonly location = inject(Location);

  protected content: HelpSection = helpContent[0];
  protected prev: HelpSection | null = null;
  protected next: HelpSection | null = null;
  protected sections: HelpSection[] = helpContent;

  constructor() {
    this.route.paramMap.subscribe(params => {
      const id = params.get('seccion');
      const found = helpContent.find(s => s.id === id);
      if (found) {
        this.content = found;
        const idx = helpContent.indexOf(found);
        this.prev = idx > 0 ? helpContent[idx - 1] : null;
        this.next = idx < helpContent.length - 1 ? helpContent[idx + 1] : null;
      }
    });
  }
}
