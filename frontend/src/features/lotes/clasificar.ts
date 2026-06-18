import type { TipoEntrega } from './api'

export const EXT_DOCUMENTO = ['pdf', 'docx', 'doc']
export const EXT_CODIGO = ['zip']

export type Clasificacion = { tipo: TipoEntrega; error: null } | { tipo: null; error: string }

export function extension(name: string): string {
  return name.slice(name.lastIndexOf('.') + 1).toLowerCase()
}

/**
 * Deriva el `tipo` de entrega del conjunto de archivos y valida coherencia
 * (espejo de la clasificación del backend) para evitar el 422: pdf/docx →
 * DOCUMENTO, zip → CODIGO, ambos → MIXTA. Cualquier otra extensión es inválida.
 */
export function clasificar(archivos: File[]): Clasificacion {
  if (archivos.length === 0) return { tipo: null, error: 'Agrega al menos un archivo.' }

  let tieneDocumento = false
  let tieneCodigo = false
  for (const archivo of archivos) {
    const ext = extension(archivo.name)
    if (EXT_DOCUMENTO.includes(ext)) tieneDocumento = true
    else if (EXT_CODIGO.includes(ext)) tieneCodigo = true
    else
      return {
        tipo: null,
        error: `Tipo de archivo no admitido: .${ext}. Usa PDF/DOCX (documento) o ZIP (código).`,
      }
  }

  if (tieneDocumento && tieneCodigo) return { tipo: 'MIXTA', error: null }
  if (tieneDocumento) return { tipo: 'DOCUMENTO', error: null }
  return { tipo: 'CODIGO', error: null }
}
