/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // Cores do tema Made in Braza (baseado no app Android)
        primary: {
          50: '#fef3e2',
          100: '#fde4b9',
          200: '#fcd48b',
          300: '#fbc35d',
          400: '#fab639',
          500: '#f9a825', // Cor principal (amarelo/dourado)
          600: '#f59100',
          700: '#ef7a00',
          800: '#e86400',
          900: '#dd4200',
        },
        dark: {
          50: '#f5f5f5',
          100: '#e0e0e0',
          200: '#bdbdbd',
          300: '#9e9e9e',
          400: '#757575',
          500: '#424242',
          600: '#303030',
          700: '#212121', // Background principal
          800: '#1a1a1a',
          900: '#121212', // Background mais escuro
        }
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [],
}
