import { acotar, familiaNivel, nivelesOrdenados } from './nivel'
import type { NivelOpcion } from './api'

describe('familiaNivel (color por posición, no por nombre)', () => {
  it('con un solo nivel devuelve bueno', () => {
    expect(familiaNivel(0, 1)).toBe('bueno')
  })

  it('mapea peor→mejor en 4 bandas', () => {
    // 4 niveles: índices 0..3
    expect(familiaNivel(0, 4)).toBe('insuficiente')
    expect(familiaNivel(1, 4)).toBe('basico')
    expect(familiaNivel(2, 4)).toBe('bueno')
    expect(familiaNivel(3, 4)).toBe('excelente')
  })

  it('en 2 niveles, el inferior es insuficiente y el superior excelente', () => {
    expect(familiaNivel(0, 2)).toBe('insuficiente')
    expect(familiaNivel(1, 2)).toBe('excelente')
  })
})

describe('acotar', () => {
  it('respeta el rango', () => {
    expect(acotar(5, 0, 10)).toBe(5)
    expect(acotar(-3, 0, 10)).toBe(0)
    expect(acotar(15, 0, 10)).toBe(10)
  })
})

describe('nivelesOrdenados', () => {
  it('ordena por orden ascendente sin mutar el original', () => {
    const niveles: NivelOpcion[] = [
      { id: 'b', nombre: 'Bueno', orden: 2, puntajeMin: 6, puntajeMax: 10 },
      { id: 'a', nombre: 'Insuficiente', orden: 1, puntajeMin: 0, puntajeMax: 5 },
    ]
    const ordenado = nivelesOrdenados(niveles)
    expect(ordenado.map((n) => n.id)).toEqual(['a', 'b'])
    expect(niveles[0].id).toBe('b') // original intacto
  })
})
