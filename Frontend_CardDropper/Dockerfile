# Étape 1 — Build Angular
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build -- --configuration production

# Étape 2 — Nginx (sert le dossier browser/)
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/Frontend_CardDropper/browser/ /usr/share/nginx/html
EXPOSE 80
