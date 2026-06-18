import { describe, expect, it } from 'vitest'

import { construirTabla, tablaACsv } from './tabla'
import type { EvaluacionAprobada } from './recolectar'

/** Mínimo borrador aprobado para los tests (solo los campos que usa la tabla). */
function aprobada(
  identificador: string,
  criterios: {
    criterioId: string
    nombreCriterio: string
    puntajeMaximo: number
    orden: number
    nivelFinalId: string | null
    puntajeFinal: number | null
    niveles: { id: string; nombre: string }[]
  }[],
  total: number | null,
  comentario: string | null,
): EvaluacionAprobada {
  return {
    entrega: { identificadorEstudiante: identificador },
    // El resto de BorradorResponse no lo toca construirTabla.
    borrador: {
      estado: 'APROBADA',
      puntajeTotalFinal: total,
      comentarioGeneral: comentario,
      criterios,
    },
  } as unknown as EvaluacionAprobada
}

const NIV = [
  { id: 'n-exc', nombre: 'Excelente' },
  { id: 'n-ins', nombre: 'Insuficiente' },
]

describe('construirTabla', () => {
  it('arma headers con dos columnas por criterio (nivel y puntaje) + total + comentario', () => {
    const a = aprobada(
      'G1',
      [
        {
          criterioId: 'c1',
          nombreCriterio: 'Correctitud',
          puntajeMaximo: 8,
          orden: 0,
          nivelFinalId: 'n-exc',
          puntajeFinal: 7,
          niveles: NIV,
        },
      ],
      18,
      'Buen trabajo',
    )
    const { headers, filas } = construirTabla([a], 20)
    expect(headers).toEqual([
      'Estudiante',
      'Correctitud — Nivel',
      'Correctitud — Puntaje (/8)',
      'Total (/20)',
      'Comentario general',
    ])
    expect(filas).toEqual([['G1', 'Excelente', 7, 18, 'Buen trabajo']])
  })

  it('alinea columnas por criterioId/orden aunque las filas lleguen desordenadas', () => {
    const cols = (nivelId: string, pts: number) => [
      {
        criterioId: 'c2',
        nombreCriterio: 'Pruebas',
        puntajeMaximo: 5,
        orden: 1,
        nivelFinalId: nivelId,
        puntajeFinal: pts,
        niveles: NIV,
      },
      {
        criterioId: 'c1',
        nombreCriterio: 'Correctitud',
        puntajeMaximo: 8,
        orden: 0,
        nivelFinalId: nivelId,
        puntajeFinal: pts,
        niveles: NIV,
      },
    ]
    const { headers, filas } = construirTabla(
      [aprobada('G1', cols('n-exc', 8), 13, null)],
      13,
    )
    // 'Correctitud' (orden 0) debe ir antes que 'Pruebas' (orden 1).
    expect(headers).toEqual([
      'Estudiante',
      'Correctitud — Nivel',
      'Correctitud — Puntaje (/8)',
      'Pruebas — Nivel',
      'Pruebas — Puntaje (/5)',
      'Total (/13)',
      'Comentario general',
    ])
    expect(filas[0]).toEqual(['G1', 'Excelente', 8, 'Excelente', 8, 13, ''])
  })

  it('deja celdas vacías cuando falta puntaje, nivel o total', () => {
    const a = aprobada(
      'G2',
      [
        {
          criterioId: 'c1',
          nombreCriterio: 'Correctitud',
          puntajeMaximo: 8,
          orden: 0,
          nivelFinalId: null,
          puntajeFinal: null,
          niveles: NIV,
        },
      ],
      null,
      null,
    )
    expect(construirTabla([a], null).filas[0]).toEqual(['G2', '', '', '', ''])
    // Sin totalMax el encabezado de total es genérico.
    expect(construirTabla([a], null).headers.at(-2)).toBe('Total')
  })
})

describe('tablaACsv', () => {
  it('escapa comas, comillas y saltos de línea, y antepone BOM', () => {
    const tabla = {
      headers: ['Estudiante', 'Comentario general'],
      filas: [['G1', 'Bien, pero "flojo"\nrevisar']] as (string | number)[][],
    }
    const csv = tablaACsv(tabla)
    expect(csv.startsWith('\ufeff')).toBe(true)
    expect(csv).toContain('"Bien, pero ""flojo""\nrevisar"')
    expect(csv.split('\r\n')).toHaveLength(2)
  })
})
