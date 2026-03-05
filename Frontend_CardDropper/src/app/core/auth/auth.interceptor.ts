import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

/**
 * Interceptor HTTP fonctionnel — attache le token JWT à chaque requête API.
 *
 * Fonctionnement :
 *   - Vérifie que la requête cible bien notre backend (apiUrl)
 *   - Si l'utilisateur est authentifié, ajoute le header Authorization: Bearer <token>
 *   - Les requêtes vers d'autres domaines (Keycloak, CDN, etc.) ne sont pas modifiées
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);

  // Ne pas injecter le token sur des requêtes qui ne ciblent pas notre API
  const isApiRequest = req.url.startsWith(environment.apiUrl);

  if (isApiRequest && auth.isAuthenticated && auth.accessToken) {
    const cloned = req.clone({
      setHeaders: { Authorization: `Bearer ${auth.accessToken}` }
    });
    return next(cloned);
  }

  return next(req);
};
