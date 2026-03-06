import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  template: `
    <header class="header">
      <div class="header__inner">
        <a routerLink="/" class="header__logo">
          <img src="assets/logos/carddropper.jpeg" class="header__logo-img"/>
          <span class="header__logo-text">CardDropper</span>
        </a>

        <nav class="header__nav">
          <a routerLink="/" routerLinkActive="header__link--active" [routerLinkActiveOptions]="{exact: true}" class="header__link">Taverne</a>
          <a routerLink="/cards" routerLinkActive="header__link--active" class="header__link">Collection</a>
          @if (auth.isAuthenticated) {
            <a routerLink="/my-cards" routerLinkActive="header__link--active" class="header__link">Mon Grimoire</a>
            <a routerLink="/create" routerLinkActive="header__link--active" class="header__link">Forge</a>
            <a routerLink="/packs" routerLinkActive="header__link--active" class="header__link">Packs</a>
            <a routerLink="/admin" routerLinkActive="header__link--active" class="header__link">Admin</a>
          }
        </nav>

        <div class="header__auth">
          @if (auth.isAuthenticated) {
            <span class="header__user">{{ auth.username }}</span>
            <button class="header__btn header__btn--outline" (click)="auth.logout()">Déconnexion</button>
          } @else {
            <button class="header__btn header__btn--gold" (click)="auth.login()">Entrer dans la taverne</button>
          }
        </div>
      </div>
      <div class="header__border"></div>
    </header>
  `,
  styles: `
    .header {
      background:
        linear-gradient(180deg, #1F1A14 0%, #161210 100%);
      position: sticky;
      top: 0;
      z-index: 100;
    }

    .header__border {
      height: 2px;
      background: linear-gradient(
        90deg,
        transparent 0%,
        var(--cd-gold-deep) 20%,
        var(--cd-gold) 50%,
        var(--cd-gold-deep) 80%,
        transparent 100%
      );
      opacity: 0.6;
    }

    .header__inner {
      max-width: 1200px;
      margin: 0 auto;
      padding: 0 1.5rem;
      height: 60px;
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .header__logo {
      display: flex;
      align-items: center;
      gap: 0.6rem;
      text-decoration: none;
      color: var(--cd-gold);
      font-weight: 700;
      font-size: 1.15rem;
      letter-spacing: 0.02em;
    }

    .header__logo-img {
      height: 34px;
      border-radius: 6px;
      box-shadow: 0 0 10px rgba(240, 178, 50, 0.2);
    }

    .header__logo-text {
      text-shadow: 0 1px 4px rgba(0, 0, 0, 0.5);
    }

    .header__nav {
      display: flex;
      gap: 0.25rem;
    }

    .header__link {
      color: var(--cd-text-muted);
      text-decoration: none;
      font-weight: 600;
      font-size: 0.85rem;
      text-transform: uppercase;
      letter-spacing: 0.06em;
      padding: 0.4rem 0.85rem;
      border-radius: 6px;
      transition: color 0.2s, background 0.2s;

      &:hover {
        color: var(--cd-text);
        background: rgba(240, 178, 50, 0.08);
      }

      &--active {
        color: var(--cd-gold) !important;
        background: rgba(240, 178, 50, 0.12);
      }
    }

    .header__auth {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }

    .header__user {
      color: var(--cd-gold-light);
      font-size: 0.85rem;
      font-weight: 600;
      text-shadow: 0 1px 3px rgba(0, 0, 0, 0.4);
    }

    .header__btn {
      border: none;
      border-radius: 6px;
      padding: 0.45rem 1.1rem;
      font-weight: 700;
      font-size: 0.8rem;
      text-transform: uppercase;
      letter-spacing: 0.04em;
      cursor: pointer;
      transition: all 0.2s;
    }

    .header__btn--gold {
      background: linear-gradient(180deg, var(--cd-gold-light) 0%, var(--cd-gold) 40%, var(--cd-gold-deep) 100%);
      color: var(--cd-text-dark);
      box-shadow: 0 2px 8px rgba(240, 178, 50, 0.3), inset 0 1px 0 rgba(255, 255, 255, 0.2);

      &:hover {
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba(240, 178, 50, 0.4);
      }
    }

    .header__btn--outline {
      background: transparent;
      color: var(--cd-text-muted);
      border: 1px solid var(--cd-border);

      &:hover {
        border-color: var(--cd-gold-deep);
        color: var(--cd-text);
      }
    }
  `,
})
export class Header {
  protected readonly auth = inject(AuthService);
}
