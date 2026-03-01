import { APP_INITIALIZER, ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideOAuthClient } from 'angular-oauth2-oidc';
import { routes } from './app.routes';
import { authInterceptor } from './core/auth/auth.interceptor';
import { AuthService } from './core/auth/auth.service';

/**
 * Configuration principale de l'application Angular.
 *
 * Providers importants :
 *   - provideRouter           : active le routing SPA
 *   - provideHttpClient       : active HttpClient + interceptor JWT
 *   - provideOAuthClient      : enregistre le client OIDC (angular-oauth2-oidc)
 *   - APP_INITIALIZER         : lance l'init OIDC AVANT le rendu de l'app
 *                                (charge le discovery document Keycloak + tente login silencieux)
 */
export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideOAuthClient(),

    // Initialise l'auth OIDC au démarrage (bloque le rendu tant que c'est pas fait)
    {
      provide: APP_INITIALIZER,
      useFactory: (auth: AuthService) => () => auth.init(),
      deps: [AuthService],
      multi: true,
    },
  ],
};
