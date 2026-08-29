/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'standalone',
  images: {
    remotePatterns: [
      { protocol: 'https', hostname: process.env.NEXT_PUBLIC_ALLOWED_IMAGE_HOSTS?.split(',')[0] || 'picsum.photos' },
      { protocol: 'https', hostname: process.env.NEXT_PUBLIC_ALLOWED_IMAGE_HOSTS?.split(',')[1] || 'images.unsplash.com' },
    ],
  },
  async rewrites() {
    const backendUrl = process.env.BACKEND_INTERNAL_URL
      || process.env.NEXT_PUBLIC_API_URL?.replace('/api/v1', '')
      || 'http://localhost:8080';
    return [
      { source: '/api/:path*', destination: `${backendUrl}/api/:path*` },
    ];
  },
};

module.exports = nextConfig;
