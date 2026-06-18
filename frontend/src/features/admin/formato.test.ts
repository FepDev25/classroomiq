import { describe, expect, it } from 'vitest'

import {
  etiquetaMes,
  formatoMoneda,
  formatoTokens,
  mesActual,
  ultimosMeses,
} from './formato'

describe('formatoMoneda', () => {
  it('formatea con símbolo de moneda estándar', () => {
    const out = formatoMoneda(12.5, 'USD')
    expect(out).toContain('12,5')
    expect(out).toMatch(/\$|US/)
  })

  it('cae a número plano con código de moneda no estándar', () => {
    expect(formatoMoneda(3, 'TOKENS')).toBe('3 TOKENS')
  })

  it('trata undefined como cero', () => {
    expect(formatoMoneda(undefined, undefined)).toBe('0')
  })
})

describe('formatoTokens', () => {
  it('agrupa miles', () => {
    expect(formatoTokens(1234567)).toBe('1.234.567')
  })
  it('trata undefined como cero', () => {
    expect(formatoTokens(undefined)).toBe('0')
  })
})

describe('etiquetaMes', () => {
  it('convierte YYYY-MM a mes legible', () => {
    expect(etiquetaMes('2026-06')).toBe('junio 2026')
  })
  it('devuelve el valor crudo si es inválido', () => {
    expect(etiquetaMes('basura')).toBe('basura')
  })
})

describe('ultimosMeses', () => {
  it('arranca en el mes actual y va hacia atrás', () => {
    const meses = ultimosMeses(3)
    expect(meses).toHaveLength(3)
    expect(meses[0]).toBe(mesActual())
    expect(meses.every((m) => /^\d{4}-\d{2}$/.test(m))).toBe(true)
  })
})
