export const environment = {
  production: true,
  // En prod, l'API est sur un sous-domaine dédié (via NPM)
  apiUrl: 'https://api.carddropperts.local',
  keycloak: {
    issuer: 'https://keycloak.andi27.synology.me/auth/realms/carddropperts',
    clientId: 'carddropper-frontend',
    // Résolu dynamiquement au runtime (window.location.origin)
    redirectUri: '',
    scope: 'openid profile email'
  }
};
