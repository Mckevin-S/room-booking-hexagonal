# Dépendances
FROM node:20-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

# Construction : l'adresse de l'API est figée dans les réécritures /api au moment du build
FROM node:20-alpine AS build
WORKDIR /app
ARG BACKEND_URL=http://backend:8080
ENV BACKEND_URL=$BACKEND_URL NEXT_TELEMETRY_DISABLED=1
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

# Exécution : serveur Next autonome
FROM node:20-alpine
WORKDIR /app
ENV NODE_ENV=production NEXT_TELEMETRY_DISABLED=1 PORT=3000 HOSTNAME=0.0.0.0
RUN addgroup -S app && adduser -S app -G app
COPY --from=build --chown=app:app /app/.next/standalone ./
COPY --from=build --chown=app:app /app/.next/static ./.next/static
USER app
EXPOSE 3000
HEALTHCHECK --interval=10s --timeout=3s --start-period=20s --retries=10 \
  CMD wget -qO- http://127.0.0.1:3000/connexion > /dev/null || exit 1
CMD ["node", "server.js"]
