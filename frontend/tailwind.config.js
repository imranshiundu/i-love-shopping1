/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{js,ts,jsx,tsx,mdx}'],
  theme: {
    extend: {
      fontFamily: {
        sans: ['Outfit', 'system-ui', 'sans-serif'],
      },
      colors: {
        primary: {
          50: '#f0f7ff', 100: '#e0efff', 200: '#b9dfff', 300: '#7cc4ff',
          400: '#36a5ff', 500: '#0c87f0', 600: '#006bcd', 700: '#0055a6',
          800: '#004989', 900: '#003d71',
        },
        brand: {
          50: '#f0fdf4', 100: '#dcfce7', 200: '#bbf7d0', 300: '#86efac',
          400: '#4ade80', 500: '#22c55e', 600: '#16a34a', 700: '#15803d',
          800: '#166534', 900: '#14532d',
        },
      },
      boxShadow: {
        'card': '0 1px 3px oklch(0.5 0.01 60 / 0.08)',
        'card-hover': '0 8px 24px oklch(0.5 0.01 60 / 0.12)',
      },
    },
  },
  plugins: [],
};
