/** @type {import('next').NextConfig} */
const backendUrl = process.env.BACKEND_URL ?? 'http://localhost:8080';

const nextConfig = {
  // Image autonome pour Docker (node server.js)
  output: 'standalone',
  reactStrictMode: true,
  poweredByHeader: false,
  // Le navigateur n'appelle que /api : le serveur Next relaie vers l'API Spring Boot (pas de CORS en production)
  async rewrites() {
    return [{ source: '/api/:path*', destination: `${backendUrl}/api/:path*` }];
  },
};

export default nextConfig;
