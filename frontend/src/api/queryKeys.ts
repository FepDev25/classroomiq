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
  entrega: (id: string) => ['entregas', id] as const,

  evaluacion: (entregaId: string) => ['entregas', entregaId, 'evaluacion'] as const,
  contenidoEntrega: (entregaId: string) => ['entregas', entregaId, 'contenido'] as const,
  similitud: (loteId: string) => ['lotes', loteId, 'similitud'] as const,
  resumen: (loteId: string) => ['lotes', loteId, 'resumen'] as const,

  usuarios: ['admin', 'usuarios'] as const,
  materiasAdmin: ['admin', 'materias'] as const,
  coordinadorMaterias: (coordinadorId: string) =>
    ['admin', 'coordinadores', coordinadorId, 'materias'] as const,

  metricasUso: (mes: string) => ['admin', 'metricas', 'uso', mes] as const,
  metricasUsoDocente: (docenteId: string, mes: string) =>
    ['admin', 'metricas', 'uso', mes, docenteId] as const,

  coordinadorMateriasList: ['coordinador', 'materias'] as const,
  coordinadorLotes: (materiaId: string) =>
    ['coordinador', 'materias', materiaId, 'lotes'] as const,
  coordinadorResumen: (loteId: string) => ['coordinador', 'lotes', loteId, 'resumen'] as const,
} as const
