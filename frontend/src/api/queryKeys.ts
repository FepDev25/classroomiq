/**
 * Claves de cache centralizadas para TanStack Query. Centralizarlas evita
 * desincronizar las invalidaciones entre features. Se van ampliando por hito.
 */
export const queryKeys = {
  sesion: ['sesion'] as const,

  materias: ['materias'] as const,
  materia: (id: string) => ['materias', id] as const,

  rubricas: (materiaId: string) => ['materias', materiaId, 'rubricas'] as const,
  rubrica: (id: string) => ['rubricas', id] as const,

  lotes: ['lotes'] as const,
  lote: (id: string) => ['lotes', id] as const,
  entregas: (loteId: string) => ['lotes', loteId, 'entregas'] as const,

  evaluacion: (entregaId: string) => ['entregas', entregaId, 'evaluacion'] as const,
  similitud: (loteId: string) => ['lotes', loteId, 'similitud'] as const,
  resumen: (loteId: string) => ['lotes', loteId, 'resumen'] as const,
} as const
