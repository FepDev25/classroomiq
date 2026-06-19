import { useState } from 'react'
import { createFileRoute, useNavigate } from '@tanstack/react-router'
import { toast } from 'sonner'

import { ApiError } from '@/api/errors'
import { useActualizarRubrica, useRubrica } from '@/features/rubricas/api'
import { rubricaToForm, type RubricaRequest } from '@/features/rubricas/form'
import { RubricaEditor } from '@/features/rubricas/rubrica-editor'
import { ErrorState, LoadingRows } from '@/components/states'

export const Route = createFileRoute('/_authed/_docente/rubricas/$rubricaId')({
  component: EditarRubricaPage,
})

function EditarRubricaPage() {
  const { rubricaId } = Route.useParams()
  const rubrica = useRubrica(rubricaId)

  if (rubrica.isPending) {
    return (
      <section className="mx-auto max-w-3xl">
        <LoadingRows rows={6} />
      </section>
    )
  }

  if (rubrica.isError || !rubrica.data.id) {
    return (
      <section className="mx-auto max-w-3xl">
        <ErrorState
          title="No pudimos cargar la rúbrica."
          message="Revisa tu conexión e inténtalo de nuevo."
          onRetry={() => rubrica.refetch()}
        />
      </section>
    )
  }

  // Una vez cargada, el editor toma los valores iniciales y la materia destino.
  return (
    <EditarRubricaForm
      rubricaId={rubricaId}
      materiaId={rubrica.data.materiaId ?? ''}
      defaultValues={rubricaToForm(rubrica.data)}
    />
  )
}

function EditarRubricaForm({
  rubricaId,
  materiaId,
  defaultValues,
}: {
  rubricaId: string
  materiaId: string
  defaultValues: ReturnType<typeof rubricaToForm>
}) {
  const navigate = useNavigate()
  const actualizar = useActualizarRubrica(rubricaId, materiaId)
  const [submitError, setSubmitError] = useState<string | undefined>(undefined)

  function onGuardar(request: RubricaRequest) {
    setSubmitError(undefined)
    actualizar.mutate(request, {
      onSuccess: () => {
        toast.success('Rúbrica actualizada')
        navigate({ to: '/materias/$materiaId', params: { materiaId } })
      },
      onError: (error: unknown) => {
        setSubmitError(error instanceof ApiError ? error.message : 'No pudimos guardar la rúbrica.')
      },
    })
  }

  return (
    <section className="mx-auto max-w-3xl">
      <RubricaEditor
        titulo="Editar rúbrica"
        descripcion="Ajusta los criterios y niveles de esta rúbrica."
        defaultValues={defaultValues}
        submitLabel="Guardar cambios"
        pending={actualizar.isPending}
        submitError={submitError}
        onGuardar={onGuardar}
        onCancel={() => navigate({ to: '/materias/$materiaId', params: { materiaId } })}
      />
    </section>
  )
}
