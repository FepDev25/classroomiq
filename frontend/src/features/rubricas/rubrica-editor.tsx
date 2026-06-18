import { useForm, useFieldArray, useFormContext, useWatch, type Control } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { ArrowDown, ArrowUp, Plus, Trash2 } from 'lucide-react'

import { rubricaFormSchema, type RubricaFormValues, type TipoPuntaje } from './schema'
import { formToRubricaRequest, nuevoCriterio, nuevoNivel, type RubricaRequest } from './form'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Switch } from '@/components/ui/switch'
import { Separator } from '@/components/ui/separator'
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'

const TIPO_LABEL: Record<TipoPuntaje, string> = {
  RANGO: 'Rango (mín–máx)',
  FIJO: 'Puntaje fijo',
  BANDA_PCT: 'Banda de %',
}

function aNumero(valor: string): number | undefined {
  if (valor.trim() === '') return undefined
  const n = Number(valor)
  return Number.isNaN(n) ? undefined : n
}

export function RubricaEditor({
  titulo,
  descripcion,
  defaultValues,
  submitLabel,
  pending,
  submitError,
  onGuardar,
  onCancel,
}: {
  titulo: string
  descripcion: string
  defaultValues: RubricaFormValues
  submitLabel: string
  pending: boolean
  submitError?: string
  onGuardar: (request: RubricaRequest) => void
  onCancel: () => void
}) {
  const form = useForm<RubricaFormValues>({
    resolver: zodResolver(rubricaFormSchema),
    defaultValues,
    mode: 'onBlur',
  })

  const criterios = useFieldArray({ control: form.control, name: 'criterios' })

  function onSubmit(values: RubricaFormValues) {
    onGuardar(formToRubricaRequest(values))
  }

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-8">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">{titulo}</h1>
          <p className="text-muted-foreground mt-1 text-sm">{descripcion}</p>
        </div>

        <div className="grid gap-4 sm:grid-cols-2">
          <FormField
            control={form.control}
            name="nombre"
            render={({ field }) => (
              <FormItem className="sm:col-span-2">
                <FormLabel>Nombre de la rúbrica</FormLabel>
                <FormControl>
                  <Input placeholder="Rúbrica de proyecto integrador" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="descripcion"
            render={({ field }) => (
              <FormItem className="sm:col-span-2">
                <FormLabel>
                  Descripción <span className="text-muted-foreground">(opcional)</span>
                </FormLabel>
                <FormControl>
                  <Textarea rows={2} placeholder="Qué evalúa esta rúbrica…" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="modoTotal"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Cálculo del total</FormLabel>
                <Select value={field.value} onValueChange={field.onChange}>
                  <FormControl>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                  </FormControl>
                  <SelectContent>
                    <SelectItem value="SUMA">Suma de criterios</SelectItem>
                    <SelectItem value="PROMEDIO">Promedio de criterios</SelectItem>
                  </SelectContent>
                </Select>
                <FormDescription>
                  Suma: los máximos de los criterios suman el total. Promedio: cada criterio vale el
                  total.
                </FormDescription>
                <FormMessage />
              </FormItem>
            )}
          />

          <NumeroField
            name="puntajeTotal"
            label="Puntaje total"
            placeholder="100"
            description="El total de la rúbrica."
          />
        </div>

        <TotalProyectado control={form.control} />

        <Separator />

        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-medium">Criterios</h2>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => criterios.append(nuevoCriterio())}
            >
              <Plus />
              Agregar criterio
            </Button>
          </div>

          {criterios.fields.map((campo, i) => (
            <CriterioCard
              key={campo.id}
              index={i}
              total={criterios.fields.length}
              onRemove={() => criterios.remove(i)}
              onMoveUp={() => criterios.move(i, i - 1)}
              onMoveDown={() => criterios.move(i, i + 1)}
            />
          ))}
        </div>

        {submitError ? (
          <p role="alert" className="text-destructive text-sm">
            {submitError}
          </p>
        ) : null}

        <div className="flex items-center justify-end gap-2">
          <Button type="button" variant="ghost" onClick={onCancel} disabled={pending}>
            Cancelar
          </Button>
          <Button type="submit" disabled={pending}>
            {pending ? 'Guardando…' : submitLabel}
          </Button>
        </div>
      </form>
    </Form>
  )
}

/** Lectura en vivo del total según el modo, espejando la regla del backend. */
function TotalProyectado({ control }: { control: Control<RubricaFormValues> }) {
  const criterios = useWatch({ control, name: 'criterios' })
  const total = useWatch({ control, name: 'puntajeTotal' })
  const modo = useWatch({ control, name: 'modoTotal' })

  const maximos = (criterios ?? []).map((c) => c?.puntajeMaximo)
  const todosDefinidos = maximos.length > 0 && maximos.every((m) => typeof m === 'number')
  const suma = maximos.reduce<number>((acc, m) => acc + (typeof m === 'number' ? m : 0), 0)

  let mensaje: string
  let coincide = true
  if (modo === 'SUMA') {
    mensaje = `Suma de criterios: ${todosDefinidos ? suma : '—'} de ${total ?? '—'}`
    coincide = !todosDefinidos || typeof total !== 'number' || Math.abs(suma - total) < 1e-6
  } else {
    mensaje = `Cada criterio debe valer ${total ?? '—'}`
  }

  return (
    <div
      className={`rounded-md border px-3 py-2 text-sm ${
        coincide ? 'border-border text-muted-foreground' : 'border-destructive/40 text-destructive'
      }`}
    >
      {mensaje}
    </div>
  )
}

function CriterioCard({
  index,
  total,
  onRemove,
  onMoveUp,
  onMoveDown,
}: {
  index: number
  total: number
  onRemove: () => void
  onMoveUp: () => void
  onMoveDown: () => void
}) {
  const { control } = useFormContext<RubricaFormValues>()
  const niveles = useFieldArray({ control, name: `criterios.${index}.niveles` })

  return (
    <div className="border-border bg-card space-y-4 rounded-lg border p-4">
      <div className="flex items-start justify-between gap-2">
        <span className="text-muted-foreground font-mono text-xs">Criterio {index + 1}</span>
        <div className="flex items-center gap-1">
          <Button
            type="button"
            variant="ghost"
            size="icon"
            onClick={onMoveUp}
            disabled={index === 0}
            aria-label="Subir criterio"
          >
            <ArrowUp />
          </Button>
          <Button
            type="button"
            variant="ghost"
            size="icon"
            onClick={onMoveDown}
            disabled={index === total - 1}
            aria-label="Bajar criterio"
          >
            <ArrowDown />
          </Button>
          <Button
            type="button"
            variant="ghost"
            size="icon"
            onClick={onRemove}
            disabled={total === 1}
            aria-label="Eliminar criterio"
          >
            <Trash2 />
          </Button>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <FormField
          control={control}
          name={`criterios.${index}.nombre`}
          render={({ field }) => (
            <FormItem>
              <FormLabel>Nombre del criterio</FormLabel>
              <FormControl>
                <Input placeholder="Análisis de complejidad" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <NumeroField
          name={`criterios.${index}.puntajeMaximo`}
          label="Puntaje máximo"
          placeholder="20"
        />
      </div>

      <FormField
        control={control}
        name={`criterios.${index}.descripcion`}
        render={({ field }) => (
          <FormItem>
            <FormLabel>
              Descripción <span className="text-muted-foreground">(la lee el modelo)</span>
            </FormLabel>
            <FormControl>
              <Textarea
                rows={2}
                placeholder="Qué debe demostrar el estudiante para cumplir este criterio…"
                {...field}
              />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />

      <FormField
        control={control}
        name={`criterios.${index}.evaluablePorContenido`}
        render={({ field }) => (
          <FormItem className="flex items-center justify-between gap-4 rounded-md border px-3 py-2">
            <div className="space-y-0.5">
              <FormLabel>Lo evalúa el modelo</FormLabel>
              <FormDescription>
                Desactívalo si requiere juicio del docente (exposición, demo): se deja en blanco
                para que lo completes.
              </FormDescription>
            </div>
            <FormControl>
              <Switch checked={field.value} onCheckedChange={field.onChange} />
            </FormControl>
          </FormItem>
        )}
      />

      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <span className="text-sm font-medium">Niveles de desempeño</span>
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => niveles.append(nuevoNivel())}
          >
            <Plus />
            Agregar nivel
          </Button>
        </div>

        {niveles.fields.map((campo, ni) => (
          <NivelRow
            key={campo.id}
            criterioIndex={index}
            index={ni}
            onRemove={() => niveles.remove(ni)}
            puedeEliminar={niveles.fields.length > 2}
          />
        ))}
      </div>
    </div>
  )
}

function NivelRow({
  criterioIndex,
  index,
  onRemove,
  puedeEliminar,
}: {
  criterioIndex: number
  index: number
  onRemove: () => void
  puedeEliminar: boolean
}) {
  const { control } = useFormContext<RubricaFormValues>()
  const base = `criterios.${criterioIndex}.niveles.${index}` as const
  const tipo = useWatch({ control, name: `${base}.tipoPuntaje` }) as TipoPuntaje

  return (
    <div className="bg-background space-y-3 rounded-md border p-3">
      <div className="flex items-end gap-2">
        <FormField
          control={control}
          name={`${base}.nombre`}
          render={({ field }) => (
            <FormItem className="flex-1">
              <FormLabel>Nivel</FormLabel>
              <FormControl>
                <Input placeholder="Excelente" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={control}
          name={`${base}.tipoPuntaje`}
          render={({ field }) => (
            <FormItem className="w-44">
              <FormLabel>Puntaje</FormLabel>
              <Select value={field.value} onValueChange={field.onChange}>
                <FormControl>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                </FormControl>
                <SelectContent>
                  {(['RANGO', 'FIJO', 'BANDA_PCT'] as const).map((t) => (
                    <SelectItem key={t} value={t}>
                      {TIPO_LABEL[t]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </FormItem>
          )}
        />
        <Button
          type="button"
          variant="ghost"
          size="icon"
          onClick={onRemove}
          disabled={!puedeEliminar}
          aria-label="Eliminar nivel"
        >
          <Trash2 />
        </Button>
      </div>

      {tipo === 'RANGO' ? (
        <div className="grid grid-cols-2 gap-3">
          <NumeroField name={`${base}.puntajeMin`} label="Mínimo" />
          <NumeroField name={`${base}.puntajeMax`} label="Máximo" />
        </div>
      ) : tipo === 'FIJO' ? (
        <div className="grid grid-cols-2 gap-3">
          <NumeroField name={`${base}.puntajeValor`} label="Puntaje" />
        </div>
      ) : (
        <div className="grid grid-cols-2 gap-3">
          <NumeroField name={`${base}.pctMin`} label="% mínimo" />
          <NumeroField name={`${base}.pctMax`} label="% máximo" />
        </div>
      )}

      <FormField
        control={control}
        name={`${base}.descripcion`}
        render={({ field }) => (
          <FormItem>
            <FormLabel>
              Descripción <span className="text-muted-foreground">(opcional)</span>
            </FormLabel>
            <FormControl>
              <Textarea
                rows={2}
                placeholder="Qué debe contener para merecer este nivel…"
                {...field}
              />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
    </div>
  )
}

/** Campo numérico (number|undefined) atado al contexto del formulario. */
function NumeroField({
  name,
  label,
  placeholder,
  description,
}: {
  name: string
  label: string
  placeholder?: string
  description?: string
}) {
  return (
    <FormField
      name={name}
      render={({ field }) => (
        <FormItem>
          <FormLabel>{label}</FormLabel>
          <FormControl>
            <Input
              type="number"
              step="any"
              inputMode="decimal"
              placeholder={placeholder}
              name={field.name}
              ref={field.ref}
              onBlur={field.onBlur}
              value={field.value ?? ''}
              onChange={(e) => field.onChange(aNumero(e.target.value))}
            />
          </FormControl>
          {description ? <FormDescription>{description}</FormDescription> : null}
          <FormMessage />
        </FormItem>
      )}
    />
  )
}
