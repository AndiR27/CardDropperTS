import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

/**
 * Interceptor HTTP fonctionnel — attache le token JWT à chaque requête API
 * et redirige vers le login si le backend renvoie un 401.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);

  // Ne pas injecter le token sur des requêtes qui ne ciblent pas notre API
  const isApiRequest = req.url.startsWith(environment.apiUrl);

  const outgoing =
    isApiRequest && auth.isAuthenticated && auth.accessToken
      ? req.clone({
          setHeaders: { Authorization: `Bearer ${auth.accessToken}` },
        })
      : req;

  return next(outgoing).pipe(
    catchError((error: HttpErrorResponse) => {
      // If backend returns 401, the token is invalid/expired — redirect to login
      if (isApiRequest && error.status === 401) {
        auth.login();
      }
      return throwError(() => error);
    }),
  );
};
