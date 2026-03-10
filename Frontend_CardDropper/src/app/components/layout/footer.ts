import { Component } from '@angular/core';

@Component({
  selector: 'app-footer',
  standalone: true,
  template: `
    <footer class="footer">
      <div class="footer__border"></div>
      <div class="footer__inner">
        <div class="footer__brand">
          <img src="assets/logos/carddropper.jpeg" class="footer__logo"/>
          <span class="footer__brand-text">CardDropper</span>
        </div>

        <div class="footer__center">
          <span class="footer__tagline">Collecte, echange, domine.</span>
          <span class="footer__copy">&copy; 2026 CardDropper</span>
        </div>

        <div class="footer__right">
          <span class="footer__badge">v1.0</span>
        </div>
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
      position: relative;
      overflow: hidden;

      &::after {
        content: '';
        position: absolute;
        top: 0;
        left: -60%;
        width: 50%;
        height: 100%;
        background: linear-gradient(90deg, transparent, rgba(240, 178, 50, 0.4), transparent);
        animation: footerShimmer 5s ease-in-out infinite;
      }
    }

    @keyframes footerShimmer {
      0%   { left: -50%; }
      50%  { left: 100%; }
      100% { left: 100%; }
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
      display: flex;
      align-items: center;
      gap: 0.5rem;
      flex-shrink: 0;
    }

    .footer__logo {
      height: 24px;
      border-radius: 4px;
      opacity: 0.6;
      transition: opacity 0.3s;

      &:hover { opacity: 0.9; }
    }

    .footer__brand-text {
      color: var(--cd-gold-deep);
      font-weight: 700;
      font-size: 0.9rem;
      letter-spacing: 0.02em;
    }

    .footer__center {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.2rem;
    }

    .footer__tagline {
      font-size: 0.82rem;
      font-weight: 600;
      font-style: italic;
      background: linear-gradient(135deg, var(--cd-gold) 0%, var(--cd-gold-deep) 100%);
      background-clip: text;
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }

    .footer__copy {
      color: var(--cd-text-muted);
      font-size: 0.7rem;
      opacity: 0.6;
    }

    .footer__right {
      display: flex;
      align-items: center;
      flex-shrink: 0;
    }

    .footer__badge {
      font-size: 0.65rem;
      font-weight: 600;
      color: var(--cd-text-muted);
      border: 1px solid rgba(255, 255, 255, 0.08);
      border-radius: 4px;
      padding: 0.15rem 0.45rem;
      opacity: 0.5;
      letter-spacing: 0.04em;
    }

    @media (max-width: 600px) {
      .footer__inner {
        flex-direction: column;
        gap: 0.75rem;
        text-align: center;
      }
    }
  `,
})
export class Footer {}
