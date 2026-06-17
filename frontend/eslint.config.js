import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import jsxA11y from 'eslint-plugin-jsx-a11y'
import tseslint from 'typescript-eslint'
import { defineConfig, globalIgnores } from 'eslint/config'

export default defineConfig([
  // Artefactos generados: nunca lintar.
  globalIgnores(['dist', 'src/routeTree.gen.ts', 'src/api/schema.d.ts']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      js.configs.recommended,
      tseslint.configs.recommended,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite,
      jsxA11y.flatConfigs.recommended,
    ],
    languageOptions: {
      globals: globals.browser,
    },
  },
  {
    // Convenciones que co-locan exports no-componente junto a componentes:
    // rutas de TanStack (`export const Route`), primitivos de shadcn
    // (variantes cva) y el provider+hook de tema. Fast-refresh no aplica.
    files: [
      'src/routes/**/*.tsx',
      'src/components/ui/**/*.tsx',
      'src/app/theme.tsx',
      'src/features/auth/auth-context.tsx',
      'src/main.tsx',
    ],
    rules: {
      'react-refresh/only-export-components': 'off',
    },
  },
])
