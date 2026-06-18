import { z } from 'zod'

/**
 * Schema del editor de rúbrica. Espeja las reglas del backend (`RubricaValidator`) para dar
 * feedback inmediato; el backend sigue siendo la autoridad (422 como red final).
 *
 * Modelo (SCHEMA.md): puntajes absolutos por criterio; total por SUMA o PROMEDIO; niveles
 * por-criterio (mín. 2) con tres formas de puntaje:
 *  - RANGO: [puntajeMin, puntajeMax] dentro de [0, puntajeMaximo del criterio]
 *  - FIJO: puntajeValor en [0, puntajeMaximo]
 *  - BANDA_PCT: [pctMin, pctMax] en [0, 100]
 */

export const TIPOS_PUNTAJE = ['RANGO', 'FIJO', 'BANDA_PCT'] as const
export const MODOS_TOTAL = ['SUMA', 'PROMEDIO'] as const

// Campo numérico opcional. Los inputs del editor ya emiten `number | undefined`
// (los vacíos se mapean a undefined en el onChange), así que no hace falta
// preprocesar — y evitar `z.preprocess` mantiene input == output del schema,
// necesario para que el tipo de `Control` cuadre con el resolver.
const numero = z.number().optional()

const nivelSchema = z.object({
  nombre: z.string().trim().min(1, 'El nivel necesita un nombre'),
  descripcion: z.string().optional(),
  tipoPuntaje: z.enum(TIPOS_PUNTAJE),
  puntajeMin: numero,
  puntajeMax: numero,
  puntajeValor: numero,
  pctMin: numero,
  pctMax: numero,
})

const criterioSchema = z
  .object({
    nombre: z.string().trim().min(1, 'El criterio necesita un nombre'),
    descripcion: z.string().optional(),
    puntajeMaximo: numero,
    evaluablePorContenido: z.boolean(),
    niveles: z.array(nivelSchema).min(2, 'Cada criterio debe tener al menos 2 niveles'),
  })
  .superRefine((criterio, ctx) => {
    const maximo = criterio.puntajeMaximo
    if (maximo === undefined || maximo <= 0) {
      ctx.addIssue({
        code: 'custom',
        path: ['puntajeMaximo'],
        message: 'Indica un puntaje máximo mayor que 0',
      })
      return
    }

    criterio.niveles.forEach((nivel, i) => {
      const at = (campo: string, message: string) =>
        ctx.addIssue({ code: 'custom', path: ['niveles', i, campo], message })

      switch (nivel.tipoPuntaje) {
        case 'RANGO': {
          if (nivel.puntajeMin === undefined) at('puntajeMin', 'Requerido')
          if (nivel.puntajeMax === undefined) at('puntajeMax', 'Requerido')
          if (nivel.puntajeMin !== undefined && nivel.puntajeMin < 0)
            at('puntajeMin', 'No puede ser negativo')
          if (
            nivel.puntajeMin !== undefined &&
            nivel.puntajeMax !== undefined &&
            nivel.puntajeMax < nivel.puntajeMin
          )
            at('puntajeMax', 'No puede ser menor que el mínimo')
          if (nivel.puntajeMax !== undefined && nivel.puntajeMax > maximo)
            at('puntajeMax', `No puede superar el máximo del criterio (${maximo})`)
          break
        }
        case 'FIJO': {
          if (nivel.puntajeValor === undefined) at('puntajeValor', 'Requerido')
          if (nivel.puntajeValor !== undefined && nivel.puntajeValor < 0)
            at('puntajeValor', 'No puede ser negativo')
          if (nivel.puntajeValor !== undefined && nivel.puntajeValor > maximo)
            at('puntajeValor', `No puede superar el máximo del criterio (${maximo})`)
          break
        }
        case 'BANDA_PCT': {
          if (nivel.pctMin === undefined) at('pctMin', 'Requerido')
          if (nivel.pctMax === undefined) at('pctMax', 'Requerido')
          if (nivel.pctMin !== undefined && nivel.pctMin < 0) at('pctMin', 'No puede ser negativo')
          if (
            nivel.pctMin !== undefined &&
            nivel.pctMax !== undefined &&
            nivel.pctMax < nivel.pctMin
          )
            at('pctMax', 'No puede ser menor que el mínimo')
          if (nivel.pctMax !== undefined && nivel.pctMax > 100) at('pctMax', 'No puede superar 100')
          break
        }
      }
    })
  })

const casiIgual = (a: number, b: number) => Math.abs(a - b) < 1e-6

export const rubricaFormSchema = z
  .object({
    nombre: z.string().trim().min(1, 'La rúbrica necesita un nombre'),
    descripcion: z.string().optional(),
    puntajeTotal: numero,
    modoTotal: z.enum(MODOS_TOTAL),
    criterios: z.array(criterioSchema).min(1, 'Agrega al menos un criterio'),
  })
  .superRefine((rubrica, ctx) => {
    const total = rubrica.puntajeTotal
    if (total === undefined || total <= 0) {
      ctx.addIssue({
        code: 'custom',
        path: ['puntajeTotal'],
        message: 'Indica un puntaje total mayor que 0',
      })
      return
    }

    const maximos = rubrica.criterios.map((c) => c.puntajeMaximo)
    if (maximos.some((m) => m === undefined)) return // ya señalado a nivel de criterio

    if (rubrica.modoTotal === 'SUMA') {
      const suma = maximos.reduce<number>((acc, m) => acc + (m ?? 0), 0)
      if (!casiIgual(suma, total)) {
        ctx.addIssue({
          code: 'custom',
          path: ['puntajeTotal'],
          message: `En modo suma, los criterios suman ${suma} y deben igualar el total (${total})`,
        })
      }
    } else {
      rubrica.criterios.forEach((c, i) => {
        if (c.puntajeMaximo !== undefined && !casiIgual(c.puntajeMaximo, total)) {
          ctx.addIssue({
            code: 'custom',
            path: ['criterios', i, 'puntajeMaximo'],
            message: `En modo promedio, cada criterio debe igualar el total (${total})`,
          })
        }
      })
    }
  })

export type RubricaFormValues = z.infer<typeof rubricaFormSchema>
export type NivelFormValues = z.infer<typeof nivelSchema>
export type TipoPuntaje = (typeof TIPOS_PUNTAJE)[number]
export type ModoTotal = (typeof MODOS_TOTAL)[number]
