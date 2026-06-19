/**
 * Enlace cita → evidencia entre los dos paneles de la revisión. El destino es el
 * fragmento citado resaltado dentro del documento completo (`panel-documento`); o,
 * como fallback cuando no hay contenido, el fragmento en la lista (`panel-entrega`).
 * En ambos casos el destino lleva este id de DOM estable y los chips de cita del
 * panel derecho hacen scroll + destello hacia él.
 */
export function evidenciaDomId(criterioEvalId: string, indice: number): string {
  return `evidencia-${criterioEvalId}-${indice}`
}

export function irAEvidencia(criterioEvalId: string, indice: number): void {
  const el = document.getElementById(evidenciaDomId(criterioEvalId, indice))
  if (!el) return
  el.scrollIntoView({ behavior: 'smooth', block: 'center' })
  el.classList.add('evidencia-flash')
  window.setTimeout(() => el.classList.remove('evidencia-flash'), 1200)
}
