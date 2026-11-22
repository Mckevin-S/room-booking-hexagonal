import type { Config } from 'tailwindcss';

/** Jetons de design de Quorum : la signalétique d'un immeuble de bureaux. */
const config: Config = {
  content: ['./src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        paper: '#F5F6F8',
        ink: { DEFAULT: '#1B2333', soft: '#4A5568', faint: '#8A94A6' },
        line: '#DFE3EA',
        signal: { DEFAULT: '#2346D8', dark: '#1A35A8', tint: '#E8EDFC' },
        present: { DEFAULT: '#16865F', tint: '#E3F4EC' },
        pending: { DEFAULT: '#E8A400', tint: '#FDF3D6', ink: '#7A5600' },
        busy: { DEFAULT: '#D5DCE8', ink: '#3B4558' },
        alert: { DEFAULT: '#C2362B', tint: '#FBE9E7' },
      },
      fontFamily: {
        sans: ['"Public Sans Variable"', 'system-ui', 'sans-serif'],
      },
      borderRadius: {
        plaque: '6px',
      },
      boxShadow: {
        plaque: '0 1px 0 rgba(27, 35, 51, 0.06), 0 0 0 1px rgba(27, 35, 51, 0.08)',
        dialog: '0 24px 48px -12px rgba(27, 35, 51, 0.35)',
      },
    },
  },
  plugins: [],
};

export default config;
