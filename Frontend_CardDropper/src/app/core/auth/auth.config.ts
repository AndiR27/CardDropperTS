import { AuthConfig } from 'angular-oauth2-oidc';
import { environment } from '../../environments/environment';

/**
 * Configuration OIDC pour Keycloak.
 *
 * - issuer : URL du realm Keycloak (discovery automatique via .well-known)
 * - clientId : client public créé dans Keycloak (pas de secret côté front)
 * - redirectUri : URL de retour après login (redirigé par Keycloak)
 * - scope : permissions demandées (openid = obligatoire, profile/email = infos user)
 * - responseType : 'code' = Authorization Code Flow (recommandé, plus sécurisé que implicit)
 * - requireHttps : désactivé en dev (localhost), Keycloak est déjà en HTTPS
 */
export const authConfig: AuthConfig = {
  issuer: environment.keycloak.issuer,
  clientId: environment.keycloak.clientId,
  redirectUri: environment.keycloak.redirectUri || window.location.origin,
  scope: environment.keycloak.scope,
  responseType: 'code',
  showDebugInformation: !environment.production,
  requireHttps: false,

  // ── Token refresh ──
  // Use refresh tokens (not iframe-based silent refresh) for Code Flow
  useSilentRefresh: false,
  // Refresh when 75% of token lifetime has elapsed (e.g. at 3:45 for a 5min token)
  timeoutFactor: 0.75,
};
