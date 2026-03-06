export const environment = {
  production: false,
  authEnabled: true,
  // URL du backend Spring Boot en local
  apiUrl: 'http://localhost:8080',
  keycloak: {
    // Realm Keycloak pour CardDropperTS
    issuer: 'https://keycloak.andi27.synology.me/auth/realms/carddropperts',
    clientId: 'carddropper-frontend',
    redirectUri: 'http://localhost:4200',
    scope: 'openid profile email'
  }
};
