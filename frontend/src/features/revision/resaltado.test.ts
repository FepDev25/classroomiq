import { describe, expect, it } from 'vitest'

import { citasResaltables, resaltar } from './resaltado'

describe('resaltar', () => {
  it('devuelve un único tramo plano cuando no hay coincidencias', () => {
    const tramos = resaltar('El informe analiza el problema.', [
      { domId: 'd1', textoCitado: 'no aparece en el texto' },
    ])
    expect(tramos).toEqual([{ texto: 'El informe analiza el problema.' }])
  })

  it('resalta el tramo exacto de una cita y deja el resto plano', () => {
    const tramos = resaltar('El informe analiza el problema con rigor.', [
      { domId: 'd1', textoCitado: 'analiza el problema' },
    ])
    expect(tramos).toEqual([
      { texto: 'El informe ' },
      { texto: 'analiza el problema', domId: 'd1' },
      { texto: ' con rigor.' },
    ])
  })

  it('empareja aunque difieran espacios/saltos de línea y mayúsculas', () => {
    // El texto del documento tiene salto de línea y otra capitalización que la cita.
    const texto = 'El informe\nAnaliza   el problema con rigor.'
    const tramos = resaltar(texto, [{ domId: 'd1', textoCitado: 'analiza el problema' }])
    const marcado = tramos.find((t) => t.domId === 'd1')
    expect(marcado?.texto).toBe('Analiza   el problema')
    // La reconstrucción es exacta: concatenar los tramos devuelve el texto original.
    expect(tramos.map((t) => t.texto).join('')).toBe(texto)
  })

  it('resalta varias citas y preserva el texto íntegro', () => {
    const texto = 'Introduce el tema, desarrolla el método y concluye con datos.'
    const tramos = resaltar(texto, [
      { domId: 'a', textoCitado: 'Introduce el tema' },
      { domId: 'b', textoCitado: 'concluye con datos' },
    ])
    expect(tramos.filter((t) => t.domId).map((t) => t.domId)).toEqual(['a', 'b'])
    expect(tramos.map((t) => t.texto).join('')).toBe(texto)
  })

  it('descarta solapes dejando la primera cita', () => {
    const texto = 'analiza el problema con rigor'
    const tramos = resaltar(texto, [
      { domId: 'primera', textoCitado: 'analiza el problema' },
      { domId: 'segunda', textoCitado: 'el problema con rigor' },
    ])
    const ids = tramos.filter((t) => t.domId).map((t) => t.domId)
    expect(ids).toEqual(['primera'])
    expect(tramos.map((t) => t.texto).join('')).toBe(texto)
  })

  it('ignora citas vacías o triviales (< 3 chars)', () => {
    expect(resaltar('texto cualquiera', [{ domId: 'x', textoCitado: '  ' }])).toEqual([
      { texto: 'texto cualquiera' },
    ])
  })

  it('devuelve vacío para texto vacío', () => {
    expect(resaltar('', [{ domId: 'x', textoCitado: 'algo' }])).toEqual([])
  })
})

describe('citasResaltables', () => {
  it('aplana las citas de los criterios con su id de DOM', () => {
    const out = citasResaltables([
      { id: 'c1', citas: [{ textoCitado: 'uno' }, { textoCitado: 'dos' }] },
      { id: 'c2', citas: [{ textoCitado: 'tres' }] },
    ])
    expect(out).toEqual([
      { domId: 'evidencia-c1-0', textoCitado: 'uno' },
      { domId: 'evidencia-c1-1', textoCitado: 'dos' },
      { domId: 'evidencia-c2-0', textoCitado: 'tres' },
    ])
  })

  it('descarta criterios sin id y citas sin texto', () => {
    const out = citasResaltables([
      { id: undefined, citas: [{ textoCitado: 'x' }] },
      { id: 'c1', citas: [{ textoCitado: undefined }, { textoCitado: 'ok' }] },
    ])
    expect(out).toEqual([{ domId: 'evidencia-c1-1', textoCitado: 'ok' }])
  })
})
