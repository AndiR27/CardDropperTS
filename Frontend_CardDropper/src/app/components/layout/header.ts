import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RouterLink],
  template: `
    <header class="header">
      <div class="header__inner">
        <a routerLink="/" class="header__logo">
          <span class="header__logo-icon">🃏</span>
          <span class="header__logo-text">CardDropper</span>
        </a>

        <nav class="header__nav">
          <a routerLink="/" class="header__link">Accueil</a>
          <a routerLink="/cards" class="header__link">Cartes</a>
          @if (auth.isAuthenticated) {
            <a routerLink="/my-cards" class="header__link">Mes Cartes</a>
            <a routerLink="/create" class="header__link">Créer</a>
            <a routerLink="/packs" class="header__link">Packs</a>
          }
        </nav>

        <div class="header__auth">
          @if (auth.isAuthenticated) {
            <span class="header__user">{{ auth.username }}</span>
            <button class="header__btn header__btn--outline" (click)="auth.logout()">Déconnexion</button>
          } @else {
            <button class="header__btn header__btn--gold" (click)="auth.login()">Se connecter</button>
          }
        </div>
      </div>
    </header>
  `,
  styles: `
    .header {
      background: var(--cd-surface);
      border-bottom: 1px solid var(--cd-border);
      position: sticky;
      top: 0;
      z-index: 100;
    }

    .header__inner {
      max-width: 1200px;
      margin: 0 auto;
      padding: 0 1.5rem;
      height: 64px;
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .header__logo {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      text-decoration: none;
      color: var(--cd-gold);
      font-weight: 700;
      font-size: 1.25rem;
    }

    .header__logo-icon { font-size: 1.5rem; }

    .header__nav {
      display: flex;
      gap: 1.5rem;
    }

    .header__link {
      color: var(--cd-text-muted);
      text-decoration: none;
      font-weight: 500;
      font-size: 0.95rem;
      transition: color 0.2s;

      &:hover { color: var(--cd-text); }
    }

    .header__auth {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }

    .header__user {
      color: var(--cd-text-muted);
      font-size: 0.9rem;
    }

    .header__btn {
      border: none;
      border-radius: 8px;
      padding: 0.5rem 1.25rem;
      font-weight: 600;
      font-size: 0.9rem;
      cursor: pointer;
      transition: all 0.2s;
    }

    .header__btn--gold {
      background: var(--cd-gold);
      color: var(--cd-text-dark);

      &:hover { background: var(--cd-gold-light); }
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
