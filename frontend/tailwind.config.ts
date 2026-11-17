/** @type {import('tailwindcss').Config} */
export default {
  darkMode: ['class'],
  content: [
    './index.html',
    './src/**/*.{js,ts,jsx,tsx}',
  ],
  theme: {
    extend: {
      colors: {
        border: 'hsl(var(--border))',
        input: 'hsl(var(--input))',
        ring: 'hsl(var(--ring))',
        background: 'hsl(var(--background))',
        foreground: 'hsl(var(--foreground))',
        primary: {
          DEFAULT: 'hsl(var(--primary))',
          foreground: 'hsl(var(--primary-foreground))',
        },
        secondary: {
          DEFAULT: 'hsl(var(--secondary))',
          foreground: 'hsl(var(--secondary-foreground))',
        },
        destructive: {
          DEFAULT: 'hsl(var(--destructive))',
          foreground: 'hsl(var(--destructive-foreground))',
        },
        muted: {
          DEFAULT: 'hsl(var(--muted))',
          foreground: 'hsl(var(--muted-foreground))',
        },
        accent: {
          DEFAULT: 'hsl(var(--accent))',
          foreground: 'hsl(var(--accent-foreground))',
        },
        popover: {
          DEFAULT: 'hsl(var(--popover))',
          foreground: 'hsl(var(--popover-foreground))',
        },
        card: {
          DEFAULT: 'hsl(var(--card))',
          foreground: 'hsl(var(--card-foreground))',
        },
        sidebar: {
          background: 'hsl(var(--sidebar-background))',
          text: 'hsl(var(--sidebar-text))',
        },
        header: {
          background: 'hsl(var(--header-background))',
        },
        table: {
          header: 'hsl(var(--table-header))',
          row: 'hsl(var(--table-row))',
          hover: 'hsl(var(--table-hover))',
        },
        school: {
          primary: 'hsl(var(--school-primary))',
          'primary-foreground': 'hsl(var(--school-primary-foreground))',
          secondary: 'hsl(var(--school-secondary))',
          'secondary-foreground': 'hsl(var(--school-secondary-foreground))',
          accent: 'hsl(var(--school-accent))',
          'accent-foreground': 'hsl(var(--school-accent-foreground))',
          background: 'hsl(var(--school-background))',
          text: 'hsl(var(--school-text))',
          card: 'hsl(var(--school-card))',
          'card-foreground': 'hsl(var(--school-card-foreground))',
          sidebar: {
            background: 'hsl(var(--school-sidebar-background))',
            text: 'hsl(var(--school-sidebar-text))',
          },
          'active-nav': {
            background: 'hsl(var(--school-active-nav-background))',
            text: 'hsl(var(--school-active-nav-text))',
          },
          border: 'hsl(var(--school-border))',
          muted: 'hsl(var(--school-muted))',
          'muted-foreground': 'hsl(var(--school-muted-foreground))',
          input: 'hsl(var(--school-input))',
          ring: 'hsl(var(--school-ring))',
        },
      },
      borderRadius: {
        lg: 'var(--radius)',
        md: 'calc(var(--radius) - 2px)',
        sm: 'calc(var(--radius) - 4px)',
      },
    },
  },
  plugins: [],
};
