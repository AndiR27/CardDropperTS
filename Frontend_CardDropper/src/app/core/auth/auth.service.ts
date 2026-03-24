import { Injectable, inject, NgZone } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { OAuthService, OAuthErrorEvent } from 'angular-oauth2-oidc';
import { firstValueFrom, filter } from 'rxjs';
import { environment } from '../../environments/environment';
import { authConfig } from './auth.config';
import type { User } from '../../models';

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
  private readonly zone = inject(NgZone);

  private _isAdmin = false;
  private _userId: number | null = null;
  private _loginInProgress = false;

  /**
   * Initialise la connexion OIDC.
   * Appelé au démarrage de l'app (APP_INITIALIZER dans app.config.ts).
   * - Charge le discovery document Keycloak (.well-known/openid-configuration)
   * - Tente de récupérer un token existant (refresh silencieux)
   * - Si authentifié, appelle GET /auth/me pour créer l'utilisateur en DB si nécessaire
   */
  async init(): Promise<void> {
    this.oauthService.configure(authConfig);

    // If we're NOT returning from a Keycloak callback but there's stale
    // OAuth state in sessionStorage from a previous failed attempt, clear
    // it so loadDiscoveryDocumentAndTryLogin() doesn't choke on it.
    const isCallback = window.location.search.includes('code=');
    if (!isCallback) {
      this.clearStaleOAuthState();
    }

    try {
      await this.oauthService.loadDiscoveryDocumentAndTryLogin();
    } catch {
      // Keycloak unreachable or stale state — full cleanup
      this.clearStaleOAuthState();
      this.oauthService.logOut(true);
      return;
    }

    // After consuming the ?code=...&state=... callback, remove the params
    // from the URL so a page reload won't re-submit the consumed code
    if (isCallback) {
      window.history.replaceState({}, document.title, window.location.pathname);
    }

    if (this.isAuthenticated) {
      this._loginInProgress = false;
      this.oauthService.setupAutomaticSilentRefresh();
      this.listenForAuthErrors();
      await this.ensureUserInDb();
    }
  }

  /**
   * Clears leftover OAuth keys from sessionStorage that logOut(true)
   * doesn't clean — PKCE verifier, nonce, state params.
   * This is what "clear browser history" does manually.
   */
  private clearStaleOAuthState(): void {
    const keysToRemove = Object.keys(sessionStorage).filter(
      k => k.startsWith('PKCE_') || k.startsWith('nonce') || k.startsWith('session_state')
    );
    keysToRemove.forEach(k => sessionStorage.removeItem(k));
  }

  /**
   * Listen for OAuth errors during the session.
   * Clears stale local storage before re-triggering login so the user
   * never needs to manually clear cache/cookies.
   */
  private listenForAuthErrors(): void {
    const recoverableErrors = [
      'token_refresh_error',
      'silent_refresh_error',
      'token_error',
      'code_error',
      'invalid_nonce_in_state',
    ];

    this.oauthService.events
      .pipe(filter((e): e is OAuthErrorEvent => e instanceof OAuthErrorEvent))
      .subscribe((e) => {
        console.warn('OAuth error event:', e.type, e);
        if (recoverableErrors.includes(e.type)) {
          // Guard against redirect loops: if a login is already in progress
          // (e.g. the redirect itself triggered another error), just bail out.
          if (this._loginInProgress) return;

          this._loginInProgress = true;
          this.oauthService.logOut(true);
          this.zone.run(() => this.login());
        }
      });
  }

  /**
   * Appelle GET /auth/me pour s'assurer que l'utilisateur existe en base.
   * Le backend crée automatiquement le user local s'il n'existe pas encore.
   */
  private async ensureUserInDb(): Promise<void> {
    try {
      const user = await firstValueFrom(this.http.get<User>(`${environment.apiUrl}/auth/me`));
      this._isAdmin = user?.admin === true;
      this._userId = user?.id ?? null;
    } catch (err) {
      console.error('Failed to sync user with backend', err);
      this._isAdmin = false;
    }
  }

  /** Redirige vers la page de login Keycloak */
  login(): void {
    this.clearStaleOAuthState();
    this.oauthService.initCodeFlow();
  }

  logout(): void {
    this._loginInProgress = false;
    this.oauthService.logOut();
  }

  /** Vérifie si l'utilisateur possède un token valide (non expiré) */
  get isAuthenticated(): boolean {
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
    const claims = this.identityClaims;
    return (claims?.['preferred_username'] as string) ?? null;
  }

  /** Returns whether the current user has admin role (set by backend during init) */
  get isAdmin(): boolean {
    return this._isAdmin;
  }

  /** Returns the current user's DB id (set during init) */
  get userId(): number | null {
    return this._userId;
  }
}
