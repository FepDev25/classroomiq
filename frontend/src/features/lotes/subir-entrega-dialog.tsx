import { useMemo, useRef, useState } from 'react'
import { toast } from 'sonner'
import { FileText, FileCode, Upload, X } from 'lucide-react'

import { ApiError } from '@/api/errors'
import { cn } from '@/lib/utils'
import { useSubirEntrega } from './hooks'
import { TipoEntregaBadge } from './badges'
import { clasificar, EXT_CODIGO, extension } from './clasificar'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

export function SubirEntregaDialog({
  loteId,
  open,
  onOpenChange,
}: {
  loteId: string
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Subir entrega</DialogTitle>
          <DialogDescription>
            Identifica al estudiante o grupo (puede ser un alias) y agrega sus archivos. El tipo se
            detecta automáticamente.
          </DialogDescription>
        </DialogHeader>
        {/* Montado solo al abrir: el estado del formulario nace fresco en cada apertura. */}
        {open ? <SubirEntregaCuerpo loteId={loteId} onOpenChange={onOpenChange} /> : null}
      </DialogContent>
    </Dialog>
  )
}

function SubirEntregaCuerpo({
  loteId,
  onOpenChange,
}: {
  loteId: string
  onOpenChange: (open: boolean) => void
}) {
  const subir = useSubirEntrega(loteId)
  const inputRef = useRef<HTMLInputElement>(null)
  const [identificador, setIdentificador] = useState('')
  const [archivos, setArchivos] = useState<File[]>([])
  const [arrastrando, setArrastrando] = useState(false)

  const clasificacion = useMemo(() => clasificar(archivos), [archivos])
  const identificadorValido = identificador.trim().length > 0
  const puedeSubir = identificadorValido && clasificacion.tipo !== null && !subir.isPending

  function agregar(nuevos: FileList | null) {
    if (!nuevos) return
    setArchivos((prev) => [...prev, ...Array.from(nuevos)])
  }

  function quitar(indice: number) {
    setArchivos((prev) => prev.filter((_, i) => i !== indice))
  }

  function onSubmit() {
    if (!puedeSubir || clasificacion.tipo === null) return
    subir.mutate(
      { identificadorEstudiante: identificador.trim(), tipo: clasificacion.tipo, archivos },
      {
        onSuccess: () => {
          toast.success('Entrega subida')
          onOpenChange(false)
        },
        onError: (error: unknown) => {
          if (error instanceof ApiError && error.status === 413) {
            toast.error('Los archivos superan el límite de tamaño permitido.')
          } else {
            toast.error(error instanceof ApiError ? error.message : 'No pudimos subir la entrega.')
          }
        },
      },
    )
  }

  return (
    <>
      <div className="space-y-4">
        <div className="space-y-2">
          <Label htmlFor="identificador">Identificador del estudiante / grupo</Label>
          <Input
            id="identificador"
            placeholder="Grupo A, alumno-03, alias…"
            value={identificador}
            onChange={(e) => setIdentificador(e.target.value)}
          />
        </div>

        <div className="space-y-2">
          <Label htmlFor="archivos-entrega">Archivos</Label>
          <button
            type="button"
            onClick={() => inputRef.current?.click()}
            onDragOver={(e) => {
              e.preventDefault()
              setArrastrando(true)
            }}
            onDragLeave={() => setArrastrando(false)}
            onDrop={(e) => {
              e.preventDefault()
              setArrastrando(false)
              agregar(e.dataTransfer.files)
            }}
            className={cn(
              'border-border flex w-full flex-col items-center gap-2 rounded-lg border border-dashed px-6 py-8 text-center transition-colors',
              'hover:border-primary/50 hover:bg-accent/40 focus-visible:ring-ring/50 focus-visible:ring-2 focus-visible:outline-none',
              arrastrando && 'border-primary bg-accent/60',
            )}
          >
            <Upload className="text-muted-foreground size-6" aria-hidden />
            <span className="text-sm font-medium">Arrastra archivos o haz clic para elegir</span>
            <span className="text-muted-foreground text-xs">
              PDF o DOCX (documento) · ZIP (código) · ambos (mixta)
            </span>
          </button>
          <input
            ref={inputRef}
            id="archivos-entrega"
            type="file"
            multiple
            accept=".pdf,.docx,.doc,.zip"
            className="sr-only"
            onChange={(e) => {
              agregar(e.target.files)
              e.target.value = ''
            }}
          />
        </div>

        {archivos.length > 0 ? (
          <ul className="space-y-1.5">
            {archivos.map((archivo, i) => {
              const esCodigo = EXT_CODIGO.includes(extension(archivo.name))
              return (
                <li
                  key={`${archivo.name}-${i}`}
                  className="border-border bg-card flex items-center gap-2 rounded-md border px-3 py-2 text-sm"
                >
                  {esCodigo ? (
                    <FileCode className="text-muted-foreground size-4 shrink-0" aria-hidden />
                  ) : (
                    <FileText className="text-muted-foreground size-4 shrink-0" aria-hidden />
                  )}
                  <span className="truncate">{archivo.name}</span>
                  <span className="text-muted-foreground ml-auto shrink-0 text-xs">
                    {(archivo.size / 1024).toFixed(0)} KB
                  </span>
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    className="size-6 shrink-0"
                    aria-label={`Quitar ${archivo.name}`}
                    onClick={() => quitar(i)}
                    disabled={subir.isPending}
                  >
                    <X className="size-3.5" />
                  </Button>
                </li>
              )
            })}
          </ul>
        ) : null}

        {clasificacion.error && archivos.length > 0 ? (
          <p className="text-destructive text-sm">{clasificacion.error}</p>
        ) : clasificacion.tipo ? (
          <p className="text-muted-foreground flex items-center gap-2 text-sm">
            Tipo detectado: <TipoEntregaBadge tipo={clasificacion.tipo} />
          </p>
        ) : null}
      </div>

      <DialogFooter>
        <Button
          type="button"
          variant="ghost"
          onClick={() => onOpenChange(false)}
          disabled={subir.isPending}
        >
          Cancelar
        </Button>
        <Button type="button" onClick={onSubmit} disabled={!puedeSubir}>
          {subir.isPending ? 'Subiendo…' : 'Subir entrega'}
        </Button>
      </DialogFooter>
    </>
  )
}
