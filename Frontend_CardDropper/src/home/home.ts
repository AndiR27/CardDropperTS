import { Component, inject, signal } from '@angular/core';
import { AuthService } from '../app/core/auth/auth.service';
import { ApiService } from '../app/shared/services/api.service';

/**
 * Page d'accueil — sert de test pour vérifier :
 *   1. La connexion Keycloak (login / logout)
 *   2. L'appel API authentifié vers le backend (/auth/me)
 */
@Component({
  selector: 'app-home',
  imports: [],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class Home {
  protected readonly auth = inject(AuthService);
  private readonly api = inject(ApiService);

  /** Résultat de l'appel /auth/me (affiché en JSON brut) */
  protected readonly meResult = signal<string>('');
  protected readonly error = signal<string>('');

  login(): void {
    this.auth.login();
  }

  logout(): void {
    this.auth.logout();
  }

  /** Appel test vers GET /auth/me pour vérifier que le token JWT fonctionne */
  testApi(): void {
    this.meResult.set('');
    this.error.set('');
    this.api.get<unknown>('/auth/me').subscribe({
      next: (data) => this.meResult.set(JSON.stringify(data, null, 2)),
      error: (err) => this.error.set(`Erreur ${err.status}: ${err.message}`),
    });
  }
}
