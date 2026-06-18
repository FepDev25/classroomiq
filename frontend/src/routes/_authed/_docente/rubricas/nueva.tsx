import { useState } from 'react'
import { createFileRoute, Link, useNavigate } from '@tanstack/react-router'
import { z } from 'zod'
import { toast } from 'sonner'
import { ArrowLeft } from 'lucide-react'

import { ApiError } from '@/api/errors'
import { useCrearRubrica } from '@/features/rubricas/api'
import { rubricaInicial, type RubricaRequest } from '@/features/rubricas/form'
import { RubricaEditor } from '@/features/rubricas/rubrica-editor'

export const Route = createFileRoute('/_authed/_docente/rubricas/nueva')({
  validateSearch: z.object({ materiaId: z.string() }),
  component: NuevaRubricaPage,
})

function NuevaRubricaPage() {
  const { materiaId } = Route.useSearch()
  const navigate = useNavigate()
  const crear = useCrearRubrica(materiaId)
  const [submitError, setSubmitError] = useState<string | undefined>(undefined)

  function onGuardar(request: RubricaRequest) {
    setSubmitError(undefined)
    crear.mutate(request, {
      onSuccess: () => {
        toast.success('Rúbrica creada')
        navigate({ to: '/materias/$materiaId', params: { materiaId } })
      },
      onError: (error: unknown) => {
        setSubmitError(error instanceof ApiError ? error.message : 'No pudimos crear la rúbrica.')
      },
    })
  }

  return (
    <section className="mx-auto max-w-3xl">
      <Link
        to="/materias/$materiaId"
        params={{ materiaId }}
        className="text-muted-foreground hover:text-foreground inline-flex items-center gap-1 text-sm"
      >
        <ArrowLeft className="size-4" />
        Volver a la materia
      </Link>
      <div className="mt-4">
        <RubricaEditor
          titulo="Nueva rúbrica"
          descripcion="Define los criterios y niveles con los que se evaluarán las entregas."
          defaultValues={rubricaInicial()}
          submitLabel="Crear rúbrica"
          pending={crear.isPending}
          submitError={submitError}
          onGuardar={onGuardar}
          onCancel={() => navigate({ to: '/materias/$materiaId', params: { materiaId } })}
        />
      </div>
    </section>
  )
}
