# ==============================================================================
# Multi-Stage Dockerfile for React Admin Frontend (Vite + Nginx)
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build React Production Bundle
# ------------------------------------------------------------------------------
FROM node:18-alpine AS builder

WORKDIR /app

# Copy dependency specifications first to leverage Docker layer caching
COPY package*.json ./
RUN npm install

# Copy application source code
COPY . .

# Pass browser-facing API URL at build time (defaults to host localhost:8080)
ARG VITE_API_BASE_URL=http://localhost:8080
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

# Build optimized production bundle
RUN npm run build

# ------------------------------------------------------------------------------
# Stage 2: Serve with Lightweight Production Nginx
# ------------------------------------------------------------------------------
FROM nginx:1.25-alpine

# Remove default Nginx welcome page
RUN rm -rf /usr/share/nginx/html/*

# Copy built static assets from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy custom Nginx configuration (handles SPA routing and /api/ reverse proxy)
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose standard HTTP port
EXPOSE 80

# Run Nginx in foreground
CMD ["nginx", "-g", "daemon off;"]
