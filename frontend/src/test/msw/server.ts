import { setupServer } from 'msw/node'

import { handlers } from './handlers'

/** Servidor MSW para los tests de Vitest (intercepta fetch en Node). */
export const server = setupServer(...handlers)
