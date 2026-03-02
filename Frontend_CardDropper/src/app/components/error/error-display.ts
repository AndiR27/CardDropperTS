import { Component, input } from '@angular/core';

@Component({
  selector: 'app-error-display',
  standalone: true,
  template: `
    @if (message()) {
      <div class="error-banner" role="alert">
        {{ message() }}
      </div>
    }
  `,
  styles: `
    .error-banner {
      padding: 0.75rem 1rem;
      border-radius: 8px;
      background: rgba(185, 28, 28, 0.2);
      color: #fbbf24;
      border: 1px solid rgba(185, 28, 28, 0.35);
    }
  `,
})
export class ErrorDisplay {
  message = input<string | null>(null);
}
