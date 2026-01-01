/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // Cores do tema Made in Braza (identico ao app Android)
        primary: {
          50: '#ffffff',
          100: '#fafafa',
          200: '#f5f5f5',
          300: '#e0e0e0',
          400: '#ffffff', // Branco
          500: '#ffffff', // Cor principal (branco - igual Android)
          600: '#e0e0e0',
          700: '#bdbdbd',
          800: '#9e9e9e',
          900: '#757575',
        },
        dark: {
          50: '#424242',
          100: '#373737',
          200: '#2c2c2c',
          300: '#242424',
          400: '#1e1e1e',
          500: '#181818',
          600: '#141414',
          700: '#121212', // Surface (igual Android)
          800: '#0a0a0a',
          900: '#000000', // Background (preto puro - igual Android)
        },
        accent: {
          50: '#e3f2fd',
          100: '#bbdefb',
          200: '#90caf9',
          300: '#64b5f6',
          400: '#42a5f5',
          500: '#2196f3', // Azul para acentos secundarios
          600: '#1e88e5',
          700: '#1976d2',
          800: '#1565c0',
          900: '#0d47a1',
        }
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [],
}
