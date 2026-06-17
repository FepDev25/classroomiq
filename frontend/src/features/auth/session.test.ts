import { afterEach, describe, expect, it } from 'vitest'

import { clearSession, getSession, getToken, setSession, type Session } from './session'
import { toSession } from './api'

function sesionValida(overrides: Partial<Session> = {}): Session {
  return {
    token: 'jwt-token',
    rol: 'DOCENTE',
    usuarioId: 'u-1',
    email: 'docente@demo.local',
    expiresAt: Date.now() + 60_000,
    ...overrides,
  }
}

describe('session', () => {
  afterEach(() => {
    clearSession()
  })

  it('persiste y recupera una sesión vigente', () => {
    setSession(sesionValida())
    expect(getSession()?.email).toBe('docente@demo.local')
    expect(getToken()).toBe('jwt-token')
  })

  it('descarta una sesión expirada y la limpia', () => {
    setSession(sesionValida({ expiresAt: Date.now() - 1 }))
    expect(getSession()).toBeNull()
    expect(getToken()).toBeNull()
    expect(localStorage.getItem('classroomiq.session')).toBeNull()
  })
})

describe('toSession', () => {
  it('mapea un TokenResponse válido a Session con expiración calculada', () => {
    const session = toSession(
      { accessToken: 'abc', rol: 'DOCENTE', usuarioId: 'u-1', expiresIn: 3600 },
      'docente@demo.local',
    )
    expect(session.token).toBe('abc')
    expect(session.rol).toBe('DOCENTE')
    expect(session.email).toBe('docente@demo.local')
    expect(session.expiresAt).toBeGreaterThan(Date.now())
  })

  it('rechaza un TokenResponse sin accessToken', () => {
    expect(() => toSession({ rol: 'DOCENTE', usuarioId: 'u-1' }, 'x@y.z')).toThrow()
  })
})
