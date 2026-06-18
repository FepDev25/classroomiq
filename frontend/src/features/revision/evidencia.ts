/**
 * Enlace cita → fragmento entre los dos paneles de la revisión. No hay texto
 * completo de la entrega en el backend (ver decisión de H4), así que la
 * "evidencia" del panel izquierdo son los fragmentos citados por el LLM,
 * agrupados por criterio. Cada fragmento tiene un id de DOM estable y los chips
 * de cita del panel derecho hacen scroll + destello hacia él.
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
