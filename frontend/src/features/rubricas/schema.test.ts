import { describe, expect, it } from 'vitest'

import { rubricaFormSchema, type RubricaFormValues } from './schema'

function nivelRango(nombre: string, min: number, max: number) {
  return {
    nombre,
    descripcion: '',
    tipoPuntaje: 'RANGO' as const,
    puntajeMin: min,
    puntajeMax: max,
    puntajeValor: undefined,
    pctMin: undefined,
    pctMax: undefined,
  }
}

function rubricaValida(): RubricaFormValues {
  return {
    nombre: 'Rúbrica de proyecto',
    descripcion: '',
    puntajeTotal: 30,
    modoTotal: 'SUMA',
    criterios: [
      {
        nombre: 'Análisis',
        descripcion: '',
        puntajeMaximo: 20,
        evaluablePorContenido: true,
        niveles: [nivelRango('Excelente', 15, 20), nivelRango('Insuficiente', 0, 14)],
      },
      {
        nombre: 'Documentación',
        descripcion: '',
        puntajeMaximo: 10,
        evaluablePorContenido: true,
        niveles: [nivelRango('Bueno', 6, 10), nivelRango('Pobre', 0, 5)],
      },
    ],
  }
}

const errorEn = (values: RubricaFormValues, pathPrefix: string) => {
  const result = rubricaFormSchema.safeParse(values)
  expect(result.success).toBe(false)
  if (result.success) return false
  return result.error.issues.some((i) => i.path.join('.').startsWith(pathPrefix))
}

describe('rubricaFormSchema', () => {
  it('acepta una rúbrica coherente (modo suma)', () => {
    expect(rubricaFormSchema.safeParse(rubricaValida()).success).toBe(true)
  })

  it('rechaza si la suma de criterios no iguala el total', () => {
    const v = rubricaValida()
    v.puntajeTotal = 50 // 20 + 10 = 30 ≠ 50
    expect(errorEn(v, 'puntajeTotal')).toBe(true)
  })

  it('en modo promedio, cada criterio debe igualar el total', () => {
    const v = rubricaValida()
    v.modoTotal = 'PROMEDIO'
    v.puntajeTotal = 20
    // criterio 1 tiene máximo 20 (ok), criterio 2 tiene 10 (≠ 20) → error en criterios.1
    expect(errorEn(v, 'criterios.1.puntajeMaximo')).toBe(true)
  })

  it('rechaza un criterio con menos de 2 niveles', () => {
    const v = rubricaValida()
    v.criterios[0].niveles = [nivelRango('Único', 0, 20)]
    expect(errorEn(v, 'criterios.0.niveles')).toBe(true)
  })

  it('rechaza un nivel RANGO cuyo máximo supera el del criterio', () => {
    const v = rubricaValida()
    v.criterios[0].niveles[0].puntajeMax = 25 // > 20
    expect(errorEn(v, 'criterios.0.niveles.0.puntajeMax')).toBe(true)
  })

  it('rechaza un nivel FIJO sin puntajeValor', () => {
    const v = rubricaValida()
    v.criterios[0].niveles[0] = {
      nombre: 'Fijo',
      descripcion: '',
      tipoPuntaje: 'FIJO',
      puntajeMin: undefined,
      puntajeMax: undefined,
      puntajeValor: undefined,
      pctMin: undefined,
      pctMax: undefined,
    }
    expect(errorEn(v, 'criterios.0.niveles.0.puntajeValor')).toBe(true)
  })

  it('rechaza un nivel BANDA_PCT con porcentaje mayor a 100', () => {
    const v = rubricaValida()
    v.criterios[0].niveles[0] = {
      nombre: 'Banda',
      descripcion: '',
      tipoPuntaje: 'BANDA_PCT',
      puntajeMin: undefined,
      puntajeMax: undefined,
      puntajeValor: undefined,
      pctMin: 90,
      pctMax: 120,
    }
    expect(errorEn(v, 'criterios.0.niveles.0.pctMax')).toBe(true)
  })

  it('acepta niveles BANDA_PCT en el rango 0..100', () => {
    const v = rubricaValida()
    v.criterios[0].niveles = [
      {
        nombre: 'Alto',
        descripcion: '',
        tipoPuntaje: 'BANDA_PCT',
        puntajeMin: undefined,
        puntajeMax: undefined,
        puntajeValor: undefined,
        pctMin: 60,
        pctMax: 100,
      },
      {
        nombre: 'Bajo',
        descripcion: '',
        tipoPuntaje: 'BANDA_PCT',
        puntajeMin: undefined,
        puntajeMax: undefined,
        puntajeValor: undefined,
        pctMin: 0,
        pctMax: 59,
      },
    ]
    expect(rubricaFormSchema.safeParse(v).success).toBe(true)
  })
})
