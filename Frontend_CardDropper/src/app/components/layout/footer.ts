import { Component } from '@angular/core';

@Component({
  selector: 'app-footer',
  standalone: true,
  template: `
    <footer class="footer">
      <div class="footer__border"></div>
      <div class="footer__inner">
        <span class="footer__brand">
          <img src="assets/logos/carddropper.jpeg" class="footer__logo"/>
          CardDropper
        </span>
        <span class="footer__copy">© 2026 — Collecte, échange, domine.</span>
      </div>
    </footer>
  `,
  styles: `
    .footer {
      background: linear-gradient(180deg, #161210 0%, #0D0B09 100%);
      margin-top: auto;
    }

    .footer__border {
      height: 1px;
      background: linear-gradient(
        90deg,
        transparent 0%,
        var(--cd-border-strong) 30%,
        var(--cd-gold-deep) 50%,
        var(--cd-border-strong) 70%,
        transparent 100%
      );
      opacity: 0.5;
    }

    .footer__inner {
      max-width: 1200px;
      margin: 0 auto;
      padding: 1.25rem 1.5rem;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .footer__brand {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      color: var(--cd-gold-deep);
      font-weight: 700;
      font-size: 0.9rem;
    }

    .footer__logo {
      height: 22px;
      border-radius: 4px;
      opacity: 0.7;
    }

    .footer__copy {
      color: var(--cd-text-muted);
      font-size: 0.8rem;
    }
  `,
})
export class Footer {}
