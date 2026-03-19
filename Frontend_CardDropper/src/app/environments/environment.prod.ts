export const environment = {
  production: true,
  // Same-origin proxy: nginx forwards /api/ to backend
  apiUrl: '/api',
  keycloak: {
    issuer: 'https://keycloak.andi27.synology.me/auth/realms/carddropperts',
    clientId: 'carddropper-frontend',
    // Resolved at runtime via window.location.origin
    redirectUri: '',
    scope: 'openid profile email'
  }
};
