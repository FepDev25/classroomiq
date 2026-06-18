import { proyectarTotal } from './total'
import type { Borrador } from './api'

function borrador(puntajes: { final?: number | null; sugerido?: number | null }[]): Borrador {
  return {
    criterios: puntajes.map((p, i) => ({
      id: String(i),
      puntajeFinal: p.final ?? null,
      puntajeSugerido: p.sugerido ?? null,
    })),
  } as Borrador
}

describe('proyectarTotal', () => {
  it('SUMA suma los puntajes', () => {
    const b = borrador([{ final: 8 }, { final: 12 }, { final: 5 }])
    expect(proyectarTotal(b, 'SUMA')).toBe(25)
  })

  it('PROMEDIO promedia los puntajes', () => {
    const b = borrador([{ final: 10 }, { final: 20 }])
    expect(proyectarTotal(b, 'PROMEDIO')).toBe(15)
  })

  it('usa el sugerido cuando no hay final', () => {
    const b = borrador([{ sugerido: 7 }, { final: 3 }])
    expect(proyectarTotal(b, 'SUMA')).toBe(10)
  })

  it('sin criterios devuelve 0', () => {
    expect(proyectarTotal(borrador([]), 'SUMA')).toBe(0)
  })

  it('trata como 0 los criterios sin puntaje (no evaluables sin llenar)', () => {
    const b = borrador([{ final: 9 }, {}])
    expect(proyectarTotal(b, 'SUMA')).toBe(9)
  })
})
