import type { components } from '@/api/schema'
import type { NivelFormValues, RubricaFormValues, TipoPuntaje } from './schema'

export type RubricaResponse = components['schemas']['RubricaResponse']
export type RubricaRequest = components['schemas']['RubricaRequest']
type NivelRequest = components['schemas']['NivelRequest']
type NivelResponse = components['schemas']['NivelResponse']

// --- Factorías de valores iniciales para el editor ---

export function nuevoNivel(nombre = ''): NivelFormValues {
  return {
    nombre,
    descripcion: '',
    tipoPuntaje: 'RANGO',
    puntajeMin: undefined,
    puntajeMax: undefined,
    puntajeValor: undefined,
    pctMin: undefined,
    pctMax: undefined,
  }
}

export function nuevoCriterio(): RubricaFormValues['criterios'][number] {
  return {
    nombre: '',
    descripcion: '',
    puntajeMaximo: undefined,
    evaluablePorContenido: true,
    niveles: [nuevoNivel('Logrado'), nuevoNivel('No logrado')],
  }
}

export function rubricaInicial(): RubricaFormValues {
  return {
    nombre: '',
    descripcion: '',
    puntajeTotal: undefined,
    modoTotal: 'SUMA',
    criterios: [nuevoCriterio()],
  }
}

// --- Mapeo respuesta del backend → valores del formulario (edición) ---

function nivelToForm(nivel: NivelResponse): NivelFormValues {
  return {
    nombre: nivel.nombre ?? '',
    descripcion: nivel.descripcion ?? '',
    tipoPuntaje: (nivel.tipoPuntaje ?? 'RANGO') as TipoPuntaje,
    puntajeMin: nivel.puntajeMin ?? undefined,
    puntajeMax: nivel.puntajeMax ?? undefined,
    puntajeValor: nivel.puntajeValor ?? undefined,
    pctMin: nivel.pctMin ?? undefined,
    pctMax: nivel.pctMax ?? undefined,
  }
}

export function rubricaToForm(rubrica: RubricaResponse): RubricaFormValues {
  return {
    nombre: rubrica.nombre ?? '',
    descripcion: rubrica.descripcion ?? '',
    puntajeTotal: rubrica.puntajeTotal ?? undefined,
    modoTotal: rubrica.modoTotal ?? 'SUMA',
    criterios: (rubrica.criterios ?? []).map((criterio) => ({
      nombre: criterio.nombre ?? '',
      descripcion: criterio.descripcion ?? '',
      puntajeMaximo: criterio.puntajeMaximo ?? undefined,
      evaluablePorContenido: criterio.evaluablePorContenido ?? true,
      niveles: (criterio.niveles ?? []).map(nivelToForm),
    })),
  }
}

// --- Mapeo valores del formulario → request (solo tras validación) ---

function nivelToRequest(nivel: NivelFormValues): NivelRequest {
  const base = {
    nombre: nivel.nombre.trim(),
    descripcion: nivel.descripcion?.trim() || undefined,
    tipoPuntaje: nivel.tipoPuntaje,
  }
  switch (nivel.tipoPuntaje) {
    case 'RANGO':
      return { ...base, puntajeMin: nivel.puntajeMin, puntajeMax: nivel.puntajeMax }
    case 'FIJO':
      return { ...base, puntajeValor: nivel.puntajeValor }
    case 'BANDA_PCT':
      return { ...base, pctMin: nivel.pctMin, pctMax: nivel.pctMax }
  }
}

export function formToRubricaRequest(values: RubricaFormValues): RubricaRequest {
  return {
    nombre: values.nombre.trim(),
    descripcion: values.descripcion?.trim() || undefined,
    // Garantizado > 0 por la validación previa.
    puntajeTotal: values.puntajeTotal as number,
    modoTotal: values.modoTotal,
    criterios: values.criterios.map((criterio) => ({
      nombre: criterio.nombre.trim(),
      descripcion: criterio.descripcion?.trim() || undefined,
      puntajeMaximo: criterio.puntajeMaximo as number,
      evaluablePorContenido: criterio.evaluablePorContenido,
      niveles: criterio.niveles.map(nivelToRequest),
    })),
  }
}
