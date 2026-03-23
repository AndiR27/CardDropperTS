import { Component } from '@angular/core';

@Component({
  selector: 'app-footer',
  standalone: true,
  template: `
    <footer class="footer">
      <div class="footer__border"></div>
      <div class="footer__inner">

        <!-- Brand -->
        <div class="footer__brand">
          <img src="assets/logos/carddropper.png" class="footer__logo"/>
          <span class="footer__brand-text">CardDropper</span>
        </div>

        <!-- Center: tagline + copyright -->
        <div class="footer__center">
          <span class="footer__tagline">Collecte, échange, domine.</span>
          <span class="footer__copy">&copy; 2026 CardDropper — All rights reserved</span>
        </div>

        <!-- Right: social links + version -->
        <div class="footer__right">
          <a href="https://github.com" target="_blank" rel="noopener" class="footer__social" aria-label="GitHub">
            <svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18">
              <path d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483
                0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466
                -.908-.62.069-.608.069-.608 1.003.07 1.531 1.032 1.531 1.032.892 1.53 2.341 1.088 2.91.832
                .092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688
                -.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004
                1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651
                .64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855
                0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017
                C22 6.484 17.522 2 12 2z"/>
            </svg>
          </a>
          <a href="https://linkedin.com" target="_blank" rel="noopener" class="footer__social" aria-label="LinkedIn">
            <svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18">
              <path d="M20.447 20.452h-3.554v-5.569c0-1.328-.027-3.037-1.852-3.037-1.853 0-2.136 1.445-2.136
                2.939v5.667H9.351V9h3.414v1.561h.046c.477-.9 1.637-1.85 3.37-1.85 3.601 0 4.267 2.37 4.267
                5.455v6.286zM5.337 7.433a2.062 2.062 0 01-2.063-2.065 2.064 2.064 0 112.063 2.065zm1.782
                13.019H3.555V9h3.564v11.452zM22.225 0H1.771C.792 0 0 .774 0 1.729v20.542C0 23.227.792 24
                1.771 24h20.451C23.2 24 24 23.227 24 22.271V1.729C24 .774 23.2 0 22.222 0h.003z"/>
            </svg>
          </a>
          <span class="footer__divider"></span>
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
      max-width: 1400px;
      margin: 0 auto;
      padding: 1.25rem 2rem;
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
      height: 26px;
      border-radius: 4px;
      opacity: 0.55;
      transition: opacity 0.3s;

      &:hover { opacity: 0.85; }
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
      font-size: 0.68rem;
      opacity: 0.5;
    }

    .footer__right {
      display: flex;
      align-items: center;
      gap: 0.6rem;
      flex-shrink: 0;
    }

    .footer__social {
      display: flex;
      align-items: center;
      justify-content: center;
      color: var(--cd-text-muted);
      opacity: 0.45;
      transition: opacity 0.2s, color 0.2s;
      text-decoration: none;

      &:hover {
        opacity: 1;
        color: var(--cd-gold);
      }
    }

    .footer__divider {
      width: 1px;
      height: 14px;
      background: rgba(255, 255, 255, 0.1);
    }

    .footer__badge {
      font-size: 0.65rem;
      font-weight: 600;
      color: var(--cd-text-muted);
      border: 1px solid rgba(255, 255, 255, 0.08);
      border-radius: 4px;
      padding: 0.15rem 0.45rem;
      opacity: 0.45;
      letter-spacing: 0.04em;
    }

    @media (max-width: 640px) {
      .footer__inner {
        flex-direction: column;
        gap: 0.85rem;
        text-align: center;
        padding: 1.25rem 1.5rem;
      }
    }
  `,
})
export class Footer {}
