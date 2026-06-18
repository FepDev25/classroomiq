import { http, HttpResponse } from 'msw'

import type { components } from '@/api/schema'

type Lote = components['schemas']['LoteResponse']
type Materia = components['schemas']['MateriaResponse']

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

export const handlers = [
  http.get(`${BASE}/api/materias`, () => HttpResponse.json(materiasFixture)),
  http.get(`${BASE}/api/lotes`, () => HttpResponse.json(lotesFixture)),
]
