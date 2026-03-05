import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { OAuthService } from 'angular-oauth2-oidc';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { authConfig } from './auth.config';

/**
 * Service d'authentification — encapsule angular-oauth2-oidc.
 *
 * Flux :
 *   1. init() configure le discovery document Keycloak + tente un login silencieux
 *   2. Si l'utilisateur n'est pas connecté, login() redirige vers Keycloak
 *   3. Le token JWT est stocké automatiquement et accessible via getToken()
 *   4. L'interceptor HTTP l'attache aux requêtes API (voir auth.interceptor.ts)
 */
@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly oauthService = inject(OAuthService);
  private readonly http = inject(HttpClient);

  /**
   * Initialise la connexion OIDC.
   * Appelé au démarrage de l'app (APP_INITIALIZER dans app.config.ts).
   * - Charge le discovery document Keycloak (.well-known/openid-configuration)
   * - Tente de récupérer un token existant (refresh silencieux)
   * - Si authentifié, appelle GET /auth/me pour créer l'utilisateur en DB si nécessaire
   */
  async init(): Promise<void> {
    if (!environment.authEnabled) {
      console.warn('[AuthService] Auth disabled — skipping Keycloak init');
      return;
    }
    this.oauthService.configure(authConfig);
    await this.oauthService.loadDiscoveryDocumentAndTryLogin();

    if (this.isAuthenticated) {
      await this.ensureUserInDb();
    }
  }

  /**
   * Appelle GET /auth/me pour s'assurer que l'utilisateur existe en base.
   * Le backend crée automatiquement le user local s'il n'existe pas encore.
   */
  private async ensureUserInDb(): Promise<void> {
    try {
      await firstValueFrom(this.http.get(`${environment.apiUrl}/auth/me`));
    } catch (err) {
      console.error('Failed to sync user with backend', err);
    }
  }

  /** Redirige vers la page de login Keycloak */
  login(): void {
    this.oauthService.initCodeFlow();
  }

  /** Déconnexion : révoque le token et redirige vers Keycloak logout */
  logout(): void {
    this.oauthService.logOut();
  }

  /** Vérifie si l'utilisateur possède un token valide (non expiré) */
  get isAuthenticated(): boolean {
    if (!environment.authEnabled) return true;
    return this.oauthService.hasValidAccessToken();
  }

  /** Retourne le token d'accès JWT (pour l'interceptor ou usage direct) */
  get accessToken(): string | null {
    return this.oauthService.getAccessToken();
  }

  /** Retourne les claims du token (username, email, etc.) */
  get identityClaims(): Record<string, unknown> | null {
    return this.oauthService.getIdentityClaims();
  }

  /** Retourne le username depuis les claims */
  get username(): string | null {
    if (!environment.authEnabled) return 'dev';
    const claims = this.identityClaims;
    return (claims?.['preferred_username'] as string) ?? null;
  }
}
