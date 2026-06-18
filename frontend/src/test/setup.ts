import '@testing-library/jest-dom/vitest'
import { afterAll, afterEach, beforeAll, vi } from 'vitest'
import { cleanup } from '@testing-library/react'

import { server } from './msw/server'

// MSW: intercepta la API en todos los tests; cada test puede sobreescribir
// handlers con server.use(). onUnhandledRequest='error' obliga a mockear todo.
beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterAll(() => server.close())

// jsdom no implementa matchMedia; lo usa el ThemeProvider.
if (!window.matchMedia) {
  window.matchMedia = vi.fn().mockImplementation((query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    addListener: vi.fn(),
    removeListener: vi.fn(),
    dispatchEvent: vi.fn(),
  }))
}

afterEach(() => {
  cleanup()
  server.resetHandlers()
})
