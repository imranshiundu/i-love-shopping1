export const config = {
  api: {
    baseUrl: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1',
  },
  recaptcha: {
    siteKey: process.env.NEXT_PUBLIC_RECAPTCHA_SITE_KEY || '',
    enabled: process.env.NEXT_PUBLIC_RECAPTCHA_ENABLED !== 'false',
  },
  oauth: {
    google: process.env.NEXT_PUBLIC_GOOGLE_ENABLED === 'true',
    github: process.env.NEXT_PUBLIC_GITHUB_ENABLED === 'true',
  },
  stripe: {
    publishableKey: process.env.NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY || '',
    minAmount: Number(process.env.NEXT_PUBLIC_STRIPE_MIN_AMOUNT) || 100,
  },
  app: {
    name: process.env.NEXT_PUBLIC_APP_NAME || 'i-love-shopping',
    description: process.env.NEXT_PUBLIC_APP_DESCRIPTION || 'B2C E-commerce Platform for the Kenyan market',
    url: process.env.NEXT_PUBLIC_APP_URL || 'http://localhost:3000',
    supportEmail: process.env.NEXT_PUBLIC_SUPPORT_EMAIL || 'support@iloveshopping.com',
    companyLocation: process.env.NEXT_PUBLIC_COMPANY_LOCATION || 'Nairobi, Kenya',
  },
  commerce: {
    defaultCountry: process.env.NEXT_PUBLIC_DEFAULT_COUNTRY || 'KE',
    defaultCurrency: process.env.NEXT_PUBLIC_CURRENCY || 'KES',
    locale: process.env.NEXT_PUBLIC_LOCALE || 'en-KE',
    freeShippingThreshold: Number(process.env.NEXT_PUBLIC_FREE_SHIPPING_THRESHOLD) || 5000,
    shippingCost: Number(process.env.NEXT_PUBLIC_SHIPPING_COST) || 10,
    taxRate: Number(process.env.NEXT_PUBLIC_TAX_RATE) || 0.16,
    minPasswordLength: Number(process.env.NEXT_PUBLIC_MIN_PASSWORD_LENGTH) || 8,
  },
  images: {
    hostnames: (process.env.NEXT_PUBLIC_ALLOWED_IMAGE_HOSTS || 'picsum.photos,images.unsplash.com').split(','),
  },
  pages: {
    featuredProducts: Number(process.env.NEXT_PUBLIC_FEATURED_PRODUCTS_COUNT) || 8,
    productsPageSize: Number(process.env.NEXT_PUBLIC_PRODUCTS_PAGE_SIZE) || 12,
    ordersPageSize: Number(process.env.NEXT_PUBLIC_ORDERS_PAGE_SIZE) || 10,
    adminPageSize: Number(process.env.NEXT_PUBLIC_ADMIN_PAGE_SIZE) || 20,
  },
} as const;
