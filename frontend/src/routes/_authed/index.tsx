import { createFileRoute } from '@tanstack/react-router'
import { Info } from 'lucide-react'

import { useAuth } from '@/features/auth/auth-context'
import { MarginMark } from '@/components/brand'

export const Route = createFileRoute('/_authed/')({
  component: Home,
})

function Home() {
  const { rol } = useAuth()

  if (rol !== 'DOCENTE') {
    return <RolNoDisponible />
  }

  return (
    <section className="mx-auto max-w-3xl py-6">
      <p className="text-muted-foreground font-mono text-xs tracking-[0.2em] uppercase">
        Panel del docente
      </p>
      <h1 className="mt-3 text-3xl font-semibold tracking-tight">Hola de nuevo</h1>
      <p className="prose-entrega text-muted-foreground mt-4 max-w-2xl text-lg">
        Desde aquí gestionarás tus materias, rúbricas y lotes de entregas, y revisarás los
        borradores de evaluación. Las primeras pantallas llegan en el próximo hito.
      </p>

      <div className="mt-8 grid gap-3 sm:grid-cols-2">
        <ProximaTarjeta
          marca="Borrador"
          color="sugerido"
          texto="El modelo propone nivel, puntaje y justificación con evidencia citada."
        />
        <ProximaTarjeta
          marca="Decisión"
          color="decision"
          texto="Tú ajustas, reescribes y apruebas. La nota final es tuya."
        />
      </div>

      <p className="text-muted-foreground mt-10 flex items-center gap-2 font-mono text-xs">
        <MarginMark className="text-primary" />
        H1 — sesión y navegación listas. Materias y rúbricas: próximo hito.
      </p>
    </section>
  )
}

function ProximaTarjeta({
  marca,
  color,
  texto,
}: {
  marca: string
  color: 'sugerido' | 'decision'
  texto: string
}) {
  const dot = color === 'sugerido' ? 'bg-sugerido' : 'bg-decision'
  return (
    <div className="border-border bg-card rounded-lg border p-4">
      <span className="flex items-center gap-2 text-sm font-medium">
        <span className={`size-2.5 rounded-full ${dot}`} aria-hidden="true" />
        {marca}
      </span>
      <p className="text-muted-foreground mt-2 text-sm">{texto}</p>
    </div>
  )
}

function RolNoDisponible() {
  return (
    <section className="mx-auto max-w-xl py-10 text-center">
      <span className="bg-secondary text-secondary-foreground mx-auto flex size-12 items-center justify-center rounded-full">
        <Info className="size-6" />
      </span>
      <h1 className="mt-4 text-2xl font-semibold tracking-tight">Tu rol llegará en breve</h1>
      <p className="text-muted-foreground prose-entrega mt-3">
        La versión web actual de classroomiq está pensada para el flujo del docente. Las vistas de
        administración y coordinación estarán disponibles próximamente.
      </p>
    </section>
  )
}
