# Build stage
FROM node:20 AS build
WORKDIR /app

# API calls go through nginx reverse proxy
ARG VITE_API_URL=http://localhost:8080
ENV VITE_API_URL=$VITE_API_URL

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

# ==========================
# Production nginx server
# ==========================
FROM nginx:stable-alpine

# Copy React production build
COPY --from=build /app/dist /usr/share/nginx/html

# Copy nginx reverse proxy configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost/index.html || exit 1

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]