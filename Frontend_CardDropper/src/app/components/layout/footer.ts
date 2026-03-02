import { Component } from '@angular/core';

@Component({
  selector: 'app-footer',
  standalone: true,
  template: `
    <footer class="footer">
      <div class="footer__inner">
        <span class="footer__brand">🃏 CardDropper</span>
        <span class="footer__copy">© 2026 — Collecte, échange, domine.</span>
      </div>
    </footer>
  `,
  styles: `
    .footer {
      background: var(--cd-surface);
      border-top: 1px solid var(--cd-border);
      margin-top: auto;
    }

    .footer__inner {
      max-width: 1200px;
      margin: 0 auto;
      padding: 1.5rem;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .footer__brand {
      color: var(--cd-gold-deep);
      font-weight: 600;
    }

    .footer__copy {
      color: var(--cd-text-muted);
      font-size: 0.85rem;
    }
  `,
})
export class Footer {}
