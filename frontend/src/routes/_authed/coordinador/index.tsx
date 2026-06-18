import { createFileRoute } from '@tanstack/react-router'

import { PortalPlaceholder } from '@/components/portal-placeholder'

export const Route = createFileRoute('/_authed/coordinador/')({
  component: CoordinadorHome,
})

function CoordinadorHome() {
  return (
    <PortalPlaceholder
      eyebrow="Coordinación"
      title="Reportes de las materias asignadas"
      nota="H0 — acceso por rol listo. Resúmenes por grupo: próximo hito."
    >
      Aquí verás los resúmenes agregados por grupo de las materias que el administrador te asignó:
      estadísticas por criterio, mapa de dominio y narrativa. Nunca trabajos ni evaluaciones
      individuales. Las pantallas llegan en un próximo hito.
    </PortalPlaceholder>
  )
}
