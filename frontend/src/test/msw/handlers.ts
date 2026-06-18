import { http, HttpResponse } from 'msw'

import type { components } from '@/api/schema'

type Lote = components['schemas']['LoteResponse']
type Materia = components['schemas']['MateriaResponse']
type MetricasUso = components['schemas']['MetricasUsoResponse']
type DocenteUsoDetalle = components['schemas']['DocenteUsoDetalleResponse']
type ResumenGrupo = components['schemas']['ResumenGrupoResponse']

// Mismo baseUrl que el cliente en tests (vite.config → test.env.VITE_API_URL).
const BASE = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

/**
 * Handlers MSW por defecto, con formas derivadas del contrato (`openapi.yaml`).
 * Los tests pueden sobreescribirlos con `server.use(...)` para casos puntuales
 * (errores, listas vacías, etc.). El `*` matchea cualquier origen.
 */
export const materiasFixture: Materia[] = [
  { id: '11111111-1111-1111-1111-111111111111', nombre: 'Algoritmos', archivada: false },
  { id: '22222222-2222-2222-2222-222222222222', nombre: 'Bases de Datos', archivada: false },
]

export const lotesFixture: Lote[] = [
  {
    id: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    materiaId: '11111111-1111-1111-1111-111111111111',
    rubricaId: '33333333-3333-3333-3333-333333333333',
    nombre: 'Proyecto Final — Grupo A',
    estado: 'ABIERTO',
  },
  {
    id: 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    materiaId: '22222222-2222-2222-2222-222222222222',
    rubricaId: '44444444-4444-4444-4444-444444444444',
    nombre: 'Tarea 1 — Normalización',
    estado: 'LISTO',
  },
]

// --- Admin · métricas de uso/costo (v2 H4)

export const metricasUsoFixture: MetricasUso = {
  mes: '2026-06',
  moneda: 'USD',
  umbralMensual: 50,
  umbralSuperado: false,
  costoTotal: 1.2345,
  totalInputTokens: 120000,
  totalOutputTokens: 45000,
  docentes: [
    {
      docenteId: 'd1111111-1111-1111-1111-111111111111',
      nombre: 'Ada Lovelace',
      email: 'ada@demo.local',
      inputTokens: 90000,
      outputTokens: 30000,
      totalTokens: 120000,
      costoEstimado: 0.9,
    },
    {
      docenteId: 'd2222222-2222-2222-2222-222222222222',
      nombre: 'Alan Turing',
      email: 'alan@demo.local',
      inputTokens: 30000,
      outputTokens: 15000,
      totalTokens: 45000,
      costoEstimado: 0.3345,
    },
  ],
}

export const docenteUsoDetalleFixture: DocenteUsoDetalle = {
  docenteId: 'd1111111-1111-1111-1111-111111111111',
  nombre: 'Ada Lovelace',
  email: 'ada@demo.local',
  mes: '2026-06',
  moneda: 'USD',
  totalInputTokens: 90000,
  totalOutputTokens: 30000,
  costoTotal: 0.9,
  porModelo: [
    {
      modelo: 'claude-sonnet-4-6',
      inputTokens: 80000,
      outputTokens: 28000,
      totalTokens: 108000,
      costoEstimado: 0.84,
    },
    {
      modelo: 'claude-haiku-4-5',
      inputTokens: 10000,
      outputTokens: 2000,
      totalTokens: 12000,
      costoEstimado: 0.06,
    },
  ],
  porOperacion: [
    {
      operacion: 'EVALUACION',
      inputTokens: 80000,
      outputTokens: 28000,
      totalTokens: 108000,
      costoEstimado: 0.84,
    },
    {
      operacion: 'NARRATIVA',
      inputTokens: 10000,
      outputTokens: 2000,
      totalTokens: 12000,
      costoEstimado: 0.06,
    },
  ],
}

// --- Coordinador · reportes de solo lectura (v2 H5)

export const resumenGrupoFixture: ResumenGrupo = {
  loteId: 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
  nombreLote: 'Tarea 1 — Normalización',
  totalEvaluaciones: 3,
  puntajeTotalRubrica: 20,
  estadisticas: {
    promedio: 15.5,
    mediana: 16,
    minima: 11,
    maxima: 19,
    histograma: [
      { etiqueta: '0–10', min: 0, max: 10, cantidad: 0 },
      { etiqueta: '11–15', min: 11, max: 15, cantidad: 1 },
      { etiqueta: '16–20', min: 16, max: 20, cantidad: 2 },
    ],
  },
  criterios: [
    {
      criterioId: 'c1111111-1111-1111-1111-111111111111',
      nombre: 'Modelo de datos',
      puntajeMaximo: 10,
      promedio: 7.5,
      promedioPct: 75,
      evaluados: 3,
      distribucion: [
        { nivelId: 'n1', nombre: 'Insuficiente', cantidad: 0, porcentaje: 0 },
        { nivelId: 'n2', nombre: 'Básico', cantidad: 1, porcentaje: 33 },
        { nivelId: 'n3', nombre: 'Bueno', cantidad: 2, porcentaje: 67 },
      ],
      sinNivel: 0,
    },
  ],
  criteriosDificiles: ['Normalización'],
  narrativa: 'El grupo demostró dominio sólido en el modelo de datos.',
}

export const handlers = [
  http.get(`${BASE}/api/materias`, () => HttpResponse.json(materiasFixture)),
  http.get(`${BASE}/api/lotes`, () => HttpResponse.json(lotesFixture)),

  http.get(`${BASE}/api/admin/metricas/uso`, () => HttpResponse.json(metricasUsoFixture)),
  http.get(`${BASE}/api/admin/metricas/uso/:docenteId`, () =>
    HttpResponse.json(docenteUsoDetalleFixture),
  ),

  http.get(`${BASE}/api/coordinador/materias`, () => HttpResponse.json(materiasFixture)),
  http.get(`${BASE}/api/coordinador/materias/:materiaId/lotes`, () =>
    HttpResponse.json(lotesFixture),
  ),
  http.get(`${BASE}/api/coordinador/lotes/:loteId/resumen`, () =>
    HttpResponse.json(resumenGrupoFixture),
  ),
]
