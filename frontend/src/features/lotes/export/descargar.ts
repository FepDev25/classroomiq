/** Utilidades compartidas por los exportadores del lote (PDF y Excel/CSV). */

/** Convierte un texto a un slug seguro para nombre de archivo. */
export function slug(texto: string): string {
  return (
    texto
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/(^-|-$)/g, '') || 'lote'
  )
}

/** Dispara la descarga de un blob en el navegador. */
export function descargar(blob: Blob, nombreArchivo: string): void {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = nombreArchivo
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}
