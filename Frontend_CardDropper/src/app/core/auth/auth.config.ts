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
  useSilentRefresh: false,
  timeoutFactor: 0.75,

  // Disable session check iframe — it detects stale session state after
  // rapid login/logout and fires error events in a loop
  sessionChecksEnabled: false,

};
